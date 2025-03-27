package no.nav.dagpenger.dataprodukter.produkter.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.dataprodukt.soknad.SoknadFaktum
import no.nav.dagpenger.dataprodukter.asUUID
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import no.nav.dagpenger.dataprodukter.person.PersonRepository
import no.nav.dagpenger.dataprodukter.søknad.Søknad
import no.nav.dagpenger.dataprodukter.søknad.SøknadRepository
import no.nav.dagpenger.dataprodukter.søknad.data.SøknadData

internal class SøknadsdataRiver(
    rapidsConnection: RapidsConnection,
    private val ferdigeSøknader: SøknadRepository,
    private val personRepository: PersonRepository,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "søker_oppgave") }
                precondition { it.requireValue("ferdig", true) }
                precondition { it.requireKey("versjon_navn") }
                validate { it.requireKey("søknad_uuid", "seksjoner", "fødselsnummer") }
            }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val søknadId = packet["søknad_uuid"].asUUID()
        val ident = packet["fødselsnummer"].asText()
        val person = personRepository.hentPerson(ident)

        if (person.harAdressebeskyttelse) return

        val søknadData = SøknadData.lagMapper(packet["seksjoner"])
        ferdigeSøknader
            .lagre(Søknad(søknadId, packet["versjon_navn"].asText(), søknadData))
            .also {
                logger.info { "Mellomlagrer data for søknadId=$søknadId" }
            }
    }
}

internal class SøknadInnsendtRiver(
    rapidsConnection: RapidsConnection,
    private val ferdigeSøknader: SøknadRepository,
    private val dataTopic: DataTopic<SoknadFaktum>,
    private val sperretFakta: List<String>,
) : River.PacketListener {
    constructor(
        rapidsConnection: RapidsConnection,
        ferdigeSøknader: SøknadRepository,
        dataTopic: DataTopic<SoknadFaktum>,
    ) : this(
        rapidsConnection,
        ferdigeSøknader,
        dataTopic,
        listOf(
            "faktum.barn-foedselsdato",
            "faktum.egen-naering-organisasjonsnummer",
            "faktum.eget-gaardsbruk-organisasjonsnummer",
        ),
    )

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "søknad_endret_tilstand") }
                precondition { it.requireValue("gjeldendeTilstand", "Innsendt") }
                validate { it.requireKey("søknad_uuid", "@opprettet") }
            }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val søknadId = packet["søknad_uuid"].asUUID()
        val opprettet = packet["@opprettet"].asLocalDateTime().toLocalDate()

        withLoggingContext(
            "søknadId" to søknadId.toString(),
            "dataprodukt" to dataTopic.topic,
        ) {
            ferdigeSøknader.hent(søknadId)?.let { (_, søknadType, data) ->
                logger.info { "Fant data for innsendt søknad" }
                data.fakta
                    .filterNot { it.beskrivendeId in sperretFakta }
                    .onEach { faktum ->
                        SoknadFaktum
                            .newBuilder()
                            .apply {
                                this.soknadId = søknadId
                                innsendtDato = opprettet
                                soknadType = søknadType
                                beskrivelse = faktum.beskrivendeId
                                type = faktum.type
                                svar = faktum.svar
                                gruppe = faktum.gruppe
                                gruppeId = faktum.gruppeId
                            }.build()
                            .also { data ->
                                dataTopic.publiser(søknadId.toString(), data)
                            }
                    }.also {
                        logger.info { "Produserte ${it.size} faktumrader" }
                    }

                ferdigeSøknader.slett(søknadId)
            } ?: logger.warn { "Manglet søknadsdata for innsendt søknad" }
        }
    }
}
