package no.nav.dagpenger.dataprodukter.produkter.behandling

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.dataprodukt.behandling.Behandling
import no.nav.dagpenger.dataprodukter.asUUID
import no.nav.dagpenger.dataprodukter.avro.asTimestamp
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import java.time.LocalDateTime

internal class VedtakRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<Behandling>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireAny("@event_name", listOf("forslag_til_vedtak", "vedtak_fattet")) }
                validate {
                    it.requireKey(
                        "@id",
                        "@opprettet",
                        "system_participating_services",
                    )
                    it.requireKey(
                        "ident",
                        "behandlingId",
                        "behandletAv",
                        "automatisk",
                        "virkningsdato",
                        "vedtakstidspunkt",
                        "fastsatt",
                        "opplysninger",
                    )
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
            val image = packet["system_participating_services"].first()["image"]?.asText() ?: ""

            val behandlere = packet.behandletAv

            val status = packet["@event_name"].asText()
            Behandling
                .newBuilder()
                .apply {
                    sekvensnummer = System.currentTimeMillis()
                    meldingsreferanseId = packet["@id"].asUUID()
                    behandlingId = packet["behandlingId"].asUUID()
                    tekniskTid = LocalDateTime.now().asTimestamp()
                    endretTid = packet["@opprettet"].asLocalDateTime().asTimestamp()
                    ident = packet["ident"].asText()
                    saksnummer = packet.saksnummer
                    behandlingType = "søknad"
                    behandlingStatus = status
                    mottattTid = packet.registrert.asTimestamp()
                    registrertTid = packet.registrert.asTimestamp()
                    ferdigBehandletTid = packet["vedtakstidspunkt"].asLocalDateTime().asTimestamp()
                    virkningsdato = packet["virkningsdato"].asLocalDate()
                    resultat = packet["fastsatt"]["utfall"].asBoolean()
                    automatisk = packet["automatisk"].asBoolean()
                    if (status == "vedtak_fattet") {
                        saksbehandler = behandlere.saksbehandler
                        beslutter = behandlere.beslutter
                    }
                    avsender = "digidag"
                    versjon = image
                }.build()
                .also { behandling ->
                    logger.info { "Publiserer rad for ${behandling::class.java.simpleName}" }
                    sikkerlogg.info { "Publiserer rad for ${behandling::class.java.simpleName}: $behandling " }

                    dataTopic.publiser(behandling)
                }
        }
    }
}

val JsonMessage.registrert: LocalDateTime get() = fagsakId["kilde"]["registrert"].asLocalDateTime()
val JsonMessage.saksnummer: String get() = fagsakId["verdi"].asText()
val JsonMessage.fagsakId: JsonNode get() = this["opplysninger"].single { it["navn"].asText() == "fagsakId" }

val JsonMessage.behandletAv: BehandletAv
    get() =
        this["behandletAv"].let { behandletAv ->
            val beslutter = behandletAv.single { it["rolle"].asText() == "beslutter" }
            val saksbehandler = behandletAv.single { it["rolle"].asText() == "saksbehandler" }
            BehandletAv(
                beslutter = beslutter["behandler"]["ident"].asText(),
                saksbehandler = saksbehandler["behandler"]["ident"].asText(),
            )
        }

data class BehandletAv(
    val beslutter: String,
    val saksbehandler: String,
)
