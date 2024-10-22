package no.nav.dagpenger.data.innlop.tjenester

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.data.innlop.SoknadIdent
import no.nav.dagpenger.data.innlop.asUUID
import no.nav.dagpenger.data.innlop.kafka.DataTopic
import no.nav.dagpenger.data.innlop.person.PersonRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class SøknadIdentRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<SoknadIdent>,
    private val personRepository: PersonRepository,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                validate { it.demandValue("@event_name", "søknad_endret_tilstand") }
                validate { it.demandValue("gjeldendeTilstand", "opprettet") }
                validate { it.requireKey("søknad_uuid", "ident") }
            }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadIdentRiver")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logger.info { "Sjekker om vi skal publisere SøknadIdent" }
        val søknadId = packet["søknad_uuid"].asUUID()
        val ident = packet["ident"].asText()

        withLoggingContext(
            "søknadId" to søknadId.toString(),
        ) {
            val person = personRepository.hentPerson(ident)

            if (person.harAdressebeskyttelse) return

            SoknadIdent
                .newBuilder()
                .apply {
                    this.soknadId = søknadId
                    this.ident = ident
                }.build()
                .also { data ->
                    logger.info { "Publiserer rad for ${data::class.java.simpleName}" }
                    sikkerlogg.info { "Publiserer rad for ${data::class.java.simpleName}: $data " }
                    dataTopic.publiser(data)
                }
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerlogg.error(problems.toExtendedReport())
    }

    override fun onSevere(
        error: MessageProblems.MessageException,
        context: MessageContext,
    ) {
        sikkerlogg.error(error.message, error.cause)
    }
}
