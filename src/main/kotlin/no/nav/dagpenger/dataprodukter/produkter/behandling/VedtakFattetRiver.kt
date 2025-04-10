package no.nav.dagpenger.dataprodukter.produkter.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import mu.withLoggingContext
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

            Vedtak
                .newBuilder()
                .apply {
                    sekvensnummer = System.currentTimeMillis()
                    meldingsreferanseId = packet["@id"].asUUID()
                    behandlingId = packet["behandlingId"].asUUID()
                    fagsakId = packet["fagsakId"].asText()
                    opprettetTid = packet["@opprettet"].asLocalDateTime().asTimestamp()
                    this.ident = ident
                    behandletHendelse =
                        BehandletHendelseIdentifikasjon(
                            packet["behandletHendelse"]["type"].asText(),
                            packet["behandletHendelse"]["id"].asText(),
                        )
                    vedtakstidspunkt = packet["vedtakstidspunkt"].asLocalDateTime().asTimestamp()
                    virkningsdato = packet["virkningsdato"].asLocalDate()
                    automatisk = packet["automatisk"].asBoolean()
                    vilkaar =
                        packet["vilkår"].map {
                            Vilkaar(
                                it["navn"].asText(),
                                it["status"].asText(),
                                it["vurderingstidspunkt"].asLocalDateTime().asTimestamp(),
                                it["hjemmel"].asText(),
                            )
                        }
                    fastsatt =
                        Fastsatt(
                            packet["fastsatt"]["utfall"].asBoolean(),
                            packet["fastsatt"]["status"]?.asText(),
                            packet["fastsatt"]["grunnlag"]?.takeUnless { it.isMissingOrNull() }?.let {
                                Grunnlag(
                                    it["grunnlag"].asDouble(),
                                    it["begrunnelse"]?.takeUnless { it.isMissingOrNull() }?.asText(),
                                )
                            },
                            packet["fastsatt"]["fastsattVanligArbeidstid"]?.takeUnless { it.isMissingOrNull() }?.let {
                                VanligArbeidstid(
                                    it["vanligArbeidstidPerUke"].asDouble(),
                                    it["nyArbeidstidPerUke"].asDouble(),
                                    it["begrunnelse"]?.takeUnless { it.isMissingOrNull() }?.asText(),
                                )
                            },
                            packet["fastsatt"]["sats"]?.takeUnless { it.isMissingOrNull() }?.let {
                                Sats(
                                    it["dagsatsMedBarnetillegg"].asInt(),
                                    it["dagsats"]?.takeUnless { it.isMissingOrNull() }?.asInt(),
                                    it["begrunnelse"]?.takeUnless { it.isMissingOrNull() }?.asText(),
                                )
                            },
                            packet["fastsatt"]["samordning"]?.map {
                                Samordning(
                                    it["type"].asText(),
                                    it["beløp"].asInt(),
                                    it["grad"].asInt(),
                                )
                            } ?: emptyList(),
                            packet["fastsatt"]["kvoter"]?.map {
                                Kvote(
                                    it["navn"].asText(),
                                    it["type"].asText(),
                                    it["verdi"].asInt(),
                                )
                            } ?: emptyList(),
                        )
                    utbetalinger = emptyList<String>()
                }.build()
                .also { vedtak ->
                    logger.info { "Publiserer rad for ${vedtak::class.java.simpleName}" }
                    sikkerlogg.info { "Publiserer rad for ${vedtak::class.java.simpleName}: $vedtak " }

                    dataTopic.publiser(ident, vedtak)
                }
        }
    }
}
