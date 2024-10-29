package no.nav.dagpenger.dataprodukter.produkter.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.dataprodukt.behandling.Behandling
import no.nav.dagpenger.dataprodukter.asUUID
import no.nav.dagpenger.dataprodukter.avro.asTimestamp
import no.nav.dagpenger.dataprodukter.kafka.DataTopic

internal class BehandlingEndretTilstandRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<Behandling>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                validate { it.demandValue("@event_name", "behandling_endret_tilstand") }
                validate {
                    it.requireKey(
                        "@id",
                        "@opprettet",
                        "ident",
                        "behandlingId",
                        "gjeldendeTilstand",
                        "system_participating_services",
                    )
                }
            }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.BehandlingEndretTilstandRiver")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        withLoggingContext(
            "behandlingId" to packet["behandlingId"].asText(),
            "dataprodukt" to dataTopic.topic,
        ) {
            val image = packet["system_participating_services"].first()["image"].asText()

            Behandling
                .newBuilder()
                .apply {
                    meldingsreferanseId = packet["@id"].asUUID()
                    behandlingId = packet["behandlingId"].asUUID()
                    tekniskTid = packet["@opprettet"].asLocalDateTime().asTimestamp()
                    endretTid = packet["@opprettet"].asLocalDateTime().asTimestamp()
                    ident = packet["ident"].asText()
                    saksnummer = "0"
                    behandlingType = "søknad"
                    behandlingStatus = packet["gjeldendeTilstand"].asText()
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
