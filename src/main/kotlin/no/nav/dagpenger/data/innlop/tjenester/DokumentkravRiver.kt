package no.nav.dagpenger.data.innlop.tjenester

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.data.innlop.Dokumentkrav
import no.nav.dagpenger.data.innlop.asUUID
import no.nav.dagpenger.data.innlop.avro.asTimestamp
import no.nav.dagpenger.data.innlop.kafka.DataTopic
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class DokumentkravRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<Dokumentkrav>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                validate { it.demandValue("@event_name", "dokumentkrav_innsendt") }
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
    ) {
        val søknadId = packet["søknad_uuid"].asUUID()

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
                        dataTopic.publiser(data)
                    }
            }
        }
    }
}
