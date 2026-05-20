package no.nav.dagpenger.dataprodukter.produkter.utbetaling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.dataprodukt.utbetaling.Utbetaling
import no.nav.dagpenger.dataprodukter.asUUID
import no.nav.dagpenger.dataprodukter.avro.asTimestamp
import no.nav.dagpenger.dataprodukter.kafka.DataTopic

internal class UtbetalingRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<Utbetaling>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireAny("@event_name", listOf("utbetaling_utført", "utbetaling_utført_historisk")) }
                validate {
                    it.requireKey("@id", "@opprettet")
                    it.requireKey(
                        "ident",
                        "behandlingId",
                        "eksternBehandlingId",
                        "sakId",
                        "eksternSakId",
                        "behandletHendelseId",
                        "behandletHendelseType",
                    )
                }
            }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.UtbetalingRiver")
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

            Utbetaling
                .newBuilder()
                .apply {
                    this.tekniskTid = packet["@opprettet"].asLocalDateTime().asTimestamp()
                    this.ident = ident

                    behandlingId = packet["behandlingId"].asUUID()
                    utbetalingId = packet["eksternBehandlingId"].asText()
                    sakId = packet["sakId"].asUUID()
                    sakIdBase64 = packet["eksternSakId"].asText()
                    behandletHendelseId = packet["behandletHendelseId"].asText()
                    behandletHendelseType = packet["behandletHendelseType"].asText()
                    opprettetTid = packet["@opprettet"].asLocalDateTime().asTimestamp()
                }.build()
                .also { utbetaling ->
                    logger.info { "Publiserer rad for ${utbetaling::class.java.simpleName}" }
                    sikkerlogg.info { "Publiserer rad for ${utbetaling::class.java.simpleName}: $utbetaling " }

                    dataTopic.publiser(ident, utbetaling)
                }
        }
    }
}
