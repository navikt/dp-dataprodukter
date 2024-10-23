package no.nav.dagpenger.data.innlop.tjenester

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.data.innlop.Ident
import no.nav.dagpenger.data.innlop.Soknadsinnlop
import no.nav.dagpenger.data.innlop.asUUID
import no.nav.dagpenger.data.innlop.avro.asTimestamp
import no.nav.dagpenger.data.innlop.kafka.DataTopic
import no.nav.dagpenger.data.innlop.person.PersonRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class SoknadsinnlopRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<Soknadsinnlop>,
    private val identTopic: DataTopic<Ident>,
    private val personRepository: PersonRepository,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                validate { it.demandValue("@event_name", "innsending_ferdigstilt") }
                validate {
                    it.requireAny(
                        "type",
                        listOf(
                            "NySøknad",
                            "Gjenopptak",
                            "Utdanning",
                            "Etablering",
                            "KlageOgAnke",
                        ),
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
                        "fagsakId",
                    )
                }
            }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SoknadsinnlopRiver")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val journalpostId = packet["journalpostId"].asText()
        val ident = packet["fødselsnummer"].asText()
        val person = personRepository.hentPerson(ident)

        withLoggingContext(
            "journalpostId" to journalpostId,
            "dataprodukt" to dataTopic.topic,
        ) {
            Soknadsinnlop
                .newBuilder()
                .apply {
                    id = packet["@id"].asUUID()
                    opprettetDato = packet["@opprettet"].asLocalDateTime().asTimestamp()
                    registrertDato = packet["datoRegistrert"].asLocalDateTime().asTimestamp()
                    this.journalpostId = journalpostId
                    skjemaKode = packet["skjemaKode"].asText()
                    tittel = packet["tittel"].asText()
                    type = packet["type"].asText()
                    fagsakId = packet["fagsakId"].asText()
                }.build()
                .also { innlop ->
                    logger.info { "Publiserer rad for ${innlop::class.java.simpleName}" }
                    sikkerlogg.info { "Publiserer rad for ${innlop::class.java.simpleName}: $innlop " }

                    dataTopic.publiser(innlop)
                }
        }

        if (person.harAdressebeskyttelse) return

        Ident
            .newBuilder()
            .apply {
                this.journalpostId = journalpostId
                this.ident = ident
            }.build()
            .also {
                identTopic.publiser(it)
                logger.info { "Publiserer rad for ${it::class.java.simpleName}" }
            }
    }
}
