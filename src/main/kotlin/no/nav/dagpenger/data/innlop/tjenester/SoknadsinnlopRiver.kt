package no.nav.dagpenger.data.innlop.tjenester

import mu.KotlinLogging
import no.nav.dagpenger.data.innlop.DataTopic
import no.nav.dagpenger.data.innlop.Soknadsinnlop
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import java.time.ZoneId

internal class SoknadsinnlopRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<Soknadsinnlop>
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
        logger.info { "Behandler pakke med id=${packet["@id"].asText()}" }
        Soknadsinnlop.newBuilder().apply {
            id = packet["@id"].asText()
            opprettetDato = packet["@opprettet"].asLocalDateTime().atZone(oslo).toInstant()
            registrertDato = packet["datoRegistrert"].asLocalDateTime().atZone(oslo).toInstant()
            journalpostId = packet["journalpostId"].asText()
            skjemaKode = packet["skjemaKode"].asText()
            tittel = packet["tittel"].asText()
            type = packet["type"].asText()
            fagsakId = packet["fagsakId"].asText()
        }.build().also { innlop ->
            logger.info { "Sender ut $innlop" }

            throw Error("Block")
            // dataTopic.publiser(innlop)
        }
    }
}
