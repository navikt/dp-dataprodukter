package no.nav.dagpenger.dataprodukter.produkter.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.dataprodukt.behandling.BehandlingEndretTilstand
import no.nav.dagpenger.dataprodukter.asUUID
import no.nav.dagpenger.dataprodukter.avro.asTimestamp
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import java.time.Duration
import java.time.LocalDateTime

internal class BehandlingEndretTilstandRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<BehandlingEndretTilstand>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behandling_endret_tilstand") }
                validate {
                    it.requireKey("@id", "@opprettet")
                    it.requireKey(
                        "ident",
                        "behandlingId",
                        "forrigeTilstand",
                        "gjeldendeTilstand",
                        "forventetFerdig",
                        "tidBrukt",
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
            val ident = packet["ident"].asText()
            val tidBrukt = Duration.parse(packet["tidBrukt"].asText())
            val forventetFerdig = packet["forventetFerdig"].asLocalDateTime()

            BehandlingEndretTilstand
                .newBuilder()
                .apply {
                    sekvensnummer = System.currentTimeMillis()
                    meldingsreferanseId = packet["@id"].asUUID()
                    behandlingId = packet["behandlingId"].asUUID()
                    this.tekniskTid = packet["@opprettet"].asLocalDateTime().asTimestamp()
                    this.ident = ident
                    forrigeTilstand = packet["forrigeTilstand"].asText()
                    gjeldendeTilstand = packet["gjeldendeTilstand"].asText()
                    if (!forventetFerdig.isEqual(LocalDateTime.MAX)) {
                        this.forventetFerdig = forventetFerdig.asTimestamp()
                    }
                    tidBruktSekund = tidBrukt.seconds
                    this.tidBrukt = tidBrukt.toString()
                }.build()
                .also { behandlingEndretTilstand ->
                    logger.info { "Publiserer rad for ${behandlingEndretTilstand::class.java.simpleName}" }
                    sikkerlogg.info { "Publiserer rad for ${behandlingEndretTilstand::class.java.simpleName}: $behandlingEndretTilstand " }

                    dataTopic.publiser(ident, behandlingEndretTilstand)
                }
        }
    }
}
