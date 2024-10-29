package no.nav.dagpenger.dataprodukter.produkter.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.dataprodukt.soknad.SoknadIdent
import no.nav.dagpenger.dataprodukter.asUUID
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import no.nav.dagpenger.dataprodukter.person.PersonRepository

internal class SøknadIdentRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<SoknadIdent>,
    private val personRepository: PersonRepository,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                validate { it.demandValue("@event_name", "søknad_endret_tilstand") }
                validate { it.requireValue("gjeldendeTilstand", "opprettet") }
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
}
