package no.nav.dagpenger.data.innlop

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.time.LocalDate

private val logger = KotlinLogging.logger { }

internal class InnlopRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic,
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "innsending_ferdigstilt") }
            validate { it.requireAny("type", listOf("NySøknad", "Gjenopptak", "Utdanning", "Etablering", "KlageOgAnke")) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        Soknadsinnlop.newBuilder().apply {
            id = packet["@id"].asText()
            opprettetDato = packet["@opprettet"].asLocalDateTime()
            registrertDato = packet["datoRegistrert"].asLocalDateTime()
            journalpostId = packet["journalpostId"].asText()
            skjemaKode = packet["skjemaKode"].asText()
            tittel = packet["tittel"].asText()
            type = packet["type"].asText()
            fagsakId = packet["fagsakId"].asText()
        }.build().also { innlop ->
            logger.info { "Sender ut $innlop" }

            dataTopic.publiser(innlop)
        }
    }
}
