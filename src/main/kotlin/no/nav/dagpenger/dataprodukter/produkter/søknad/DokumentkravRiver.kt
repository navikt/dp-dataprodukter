package no.nav.dagpenger.dataprodukter.produkter.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.dataprodukt.soknad.Dokumentkrav
import no.nav.dagpenger.dataprodukter.asUUID
import no.nav.dagpenger.dataprodukter.avro.asTimestamp
import no.nav.dagpenger.dataprodukter.kafka.DataTopic

internal class DokumentkravRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<Dokumentkrav>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "dokumentkrav_innsendt") }
                validate {
                    it.requireKey(
                        "søknad_uuid",
                        "ident",
                        "søknadType",
                        "innsendingsType",
                        "innsendttidspunkt",
                        "hendelseId",
                    )
                }
                validate {
                    it.requireArray("dokumentkrav") {
                        requireKey("dokumentnavn", "skjemakode", "valg")
                    }
                }
            }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.DokumentkravRiver")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val søknadId = packet["søknad_uuid"].asUUID()
        val ident = packet["ident"].asText()

        withLoggingContext("søknadId" to søknadId.toString()) {
            val dokumentkrav =
                Dokumentkrav.newBuilder().apply {
                    soknadId = søknadId
                    soknadType = packet["søknadType"].asText()
                    innsendingstype = packet["innsendingsType"].asText()
                    innsendttidspunkt = packet["innsendttidspunkt"].asLocalDateTime().asTimestamp()
                    ferdigBesvart = packet["dokumentkrav"].none { it["valg"].asText() == "SEND_SENERE" }
                    hendelseId = packet["hendelseId"].asUUID()
                }

            packet["dokumentkrav"].map {
                Dokumentkrav
                    .newBuilder(dokumentkrav)
                    .apply {
                        dokumentnavn = it["dokumentnavn"].asText()
                        skjemakode = it["skjemakode"].asText()
                        valg = it["valg"].asText()
                    }.build()
                    .also { data ->
                        logger.info { "Publiserer rad for ${data::class.java.simpleName}" }
                        sikkerlogg.info { "Publiserer rad for ${data::class.java.simpleName}: $data " }
                        dataTopic.publiser(ident, data)
                    }
            }
        }
    }
}
