package no.nav.dagpenger.dataprodukter.produkter.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behandling.api.models.VedtakDTO
import no.nav.dagpenger.dataprodukt.behandling.BehandletHendelseIdentifikasjon
import no.nav.dagpenger.dataprodukt.behandling.Fastsatt
import no.nav.dagpenger.dataprodukt.behandling.Grunnlag
import no.nav.dagpenger.dataprodukt.behandling.Kvote
import no.nav.dagpenger.dataprodukt.behandling.Samordning
import no.nav.dagpenger.dataprodukt.behandling.Sats
import no.nav.dagpenger.dataprodukt.behandling.VanligArbeidstid
import no.nav.dagpenger.dataprodukt.behandling.Vedtak
import no.nav.dagpenger.dataprodukt.behandling.Vilkaar
import no.nav.dagpenger.dataprodukter.asUUID
import no.nav.dagpenger.dataprodukter.avro.asTimestamp
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import no.nav.dagpenger.dataprodukter.objectMapper

internal class VedtakFattetRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<Vedtak>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireAny("@event_name", listOf("vedtak_fattet")) }
                validate {
                    it.requireKey("@id", "@opprettet")
                    it.requireKey(
                        "ident",
                        "behandlingId",
                        "behandletHendelse",
                        "fagsakId",
                        "automatisk",
                        "virkningsdato",
                        "vedtakstidspunkt",
                        "vilkår",
                        "fastsatt",
                        "opplysninger",
                    )
                    it.interestedIn("behandletAv")
                }
            }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.VedtakFattetRiver")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        withLoggingContext(
            "behandlingId" to packet["behandlingId"].asText(),
            "dataprodukt" to dataTopic.topic,
        ) {
            val ident = packet["ident"].asText()

            if (!packet.harBehandletAv && !packet["automatisk"].asBoolean()) {
                sikkerlogg.warn { "Vedtaket er ikke automatisk, men mangler behandlet av: ${packet.toJson()}" }
            }

            val vedtak = objectMapper.readValue(packet.toJson(), VedtakDTO::class.java)

            Vedtak
                .newBuilder()
                .apply {
                    sekvensnummer = System.currentTimeMillis()
                    meldingsreferanseId = packet["@id"].asUUID()
                    behandlingId = vedtak.behandlingId
                    fagsakId = packet["fagsakId"].asText()
                    opprettetTid = packet["@opprettet"].asLocalDateTime().asTimestamp()
                    this.ident = ident
                    behandletHendelse =
                        BehandletHendelseIdentifikasjon(
                            vedtak.behandletHendelse.type.value,
                            vedtak.behandletHendelse.id,
                        )
                    vedtakstidspunkt = vedtak.vedtakstidspunkt.asTimestamp()
                    virkningsdato = vedtak.virkningsdato
                    automatisk = vedtak.automatisk == true
                    vilkaar =
                        vedtak.vilkår.map {
                            Vilkaar(
                                it.navn.value,
                                it.status.value,
                                it.vurderingstidspunkt.asTimestamp(),
                                it.hjemmel,
                            )
                        }
                    fastsatt =
                        Fastsatt(
                            vedtak.fastsatt.utfall,
                            vedtak.fastsatt.status?.value,
                            vedtak.fastsatt.grunnlag?.let {
                                Grunnlag(
                                    it.grunnlag,
                                    it.begrunnelse?.toString(),
                                )
                            },
                            vedtak.fastsatt.fastsattVanligArbeidstid?.let {
                                VanligArbeidstid(
                                    it.vanligArbeidstidPerUke.toDouble(),
                                    it.nyArbeidstidPerUke.toDouble(),
                                    it.begrunnelse?.toString(),
                                )
                            },
                            vedtak.fastsatt.sats?.let {
                                Sats(
                                    it.dagsatsMedBarnetillegg,
                                    it.begrunnelse?.toString(),
                                )
                            },
                            vedtak.fastsatt.samordning?.map {
                                Samordning(
                                    it.type,
                                    it.beløp.toInt(),
                                    it.grad.toInt(),
                                )
                            } ?: emptyList(),
                            vedtak.fastsatt.kvoter?.map {
                                Kvote(
                                    it.navn,
                                    it.type?.value,
                                    it.verdi?.toInt(),
                                )
                            } ?: emptyList(),
                        )
                }.build()
                .also { vedtak ->
                    logger.info { "Publiserer rad for ${vedtak::class.java.simpleName}" }
                    sikkerlogg.info { "Publiserer rad for ${vedtak::class.java.simpleName}: $vedtak " }

                    dataTopic.publiser(ident, vedtak)
                }
        }
    }
}
