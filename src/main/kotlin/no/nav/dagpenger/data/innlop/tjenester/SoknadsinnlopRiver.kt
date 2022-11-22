package no.nav.dagpenger.data.innlop.tjenester

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.dagpenger.data.innlop.DataTopic
import no.nav.dagpenger.data.innlop.Ident
import no.nav.dagpenger.data.innlop.Soknadsinnlop
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import java.time.ZoneId
import java.util.UUID

internal class SoknadsinnlopRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<Soknadsinnlop>,
    private val identTopic: DataTopic<Ident>
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "innsending_ferdigstilt") }
            validate {
                it.requireAny(
                    "type",
                    listOf(
                        "NySøknad",
                        "Gjenopptak",
                        "Utdanning",
                        "Etablering",
                        "KlageOgAnke"
                    )
                )
            }
            validate {
                it.interestedIn(
                    "@id",
                    "@opprettet",
                    "datoRegistrert",
                    "fødselsnummer",
                    "journalpostId",
                    "skjemaKode",
                    "tittel",
                    "fagsakId"
                )
            }
        }.register(this)
    }

    companion object {
        private val oslo: ZoneId = ZoneId.of("Europe/Oslo")
        private val logger = KotlinLogging.logger { }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val journalpostId = packet["journalpostId"].asText()

        Soknadsinnlop.newBuilder().apply {
            id = packet["@id"].asUUID()
            opprettetDato = packet["@opprettet"].asLocalDateTime().atZone(oslo).toInstant()
            registrertDato = packet["datoRegistrert"].asLocalDateTime().atZone(oslo).toInstant()
            this.journalpostId = journalpostId
            skjemaKode = packet["skjemaKode"].asText()
            tittel = packet["tittel"].asText()
            type = packet["type"].asText()
            fagsakId = packet["fagsakId"].asText()
        }.build().also { innlop ->
            logger.info { "Sender ut $innlop" }

            dataTopic.publiser(innlop)
        }

        Ident.newBuilder().apply {
            this.journalpostId = journalpostId
            ident = packet["fødselsnummer"].asText()
        }.build().also {
            identTopic.publiser(it)
        }
    }
}

private fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }
