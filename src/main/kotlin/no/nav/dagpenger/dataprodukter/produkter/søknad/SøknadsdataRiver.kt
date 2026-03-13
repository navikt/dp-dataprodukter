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
import kotlin.math.log
import no.nav.dagpenger.dataprodukt.soknad.OrkestratorSoknad
import no.nav.dagpenger.dataprodukt.soknad.Seksjonsinfo
import no.nav.dagpenger.dataprodukt.soknad.SoknadFaktum
import no.nav.dagpenger.dataprodukter.asUUID
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import no.nav.dagpenger.dataprodukter.objectMapper
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
                precondition { it.forbid("kilde")}
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

internal class OrkestratorSøknadsdataRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<OrkestratorSoknad>,
    ) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "søknad_endret_tilstand") }
                precondition { it.requireValue("gjeldendeTilstand", "Innsendt") }
                precondition { it.requireValue("kilde", "orkestrator") }
                validate { it.requireKey("søknad_uuid", "@opprettet", "søknadsdata") }
            }.register(this)
    }
    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.OrkestratorSøknadsdataRiver")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val søknadId = packet["søknad_uuid"].asUUID()
        val opprettet = packet["@opprettet"].asLocalDateTime()

        val søknadsdataPacket = packet["søknadsdata"]
        withLoggingContext(
            "søknadId" to søknadId.toString(),
            "dataprodukt" to "orkestrator-søknadsdata",
        ) {
            logger.info { "Mottok innsendt søknad fra orkestrator, søknadId=$søknadId opprettet=$opprettet" }
            sikkerlogg.info { "Mottok innsendt søknad fra orkestrator, søknadId=$søknadId opprettet=$opprettet, søknadsdata=${søknadsdataPacket.toPrettyString()}" }

            val opprettetTid = søknadsdataPacket["opprettet"].asText().takeIf { it != "null" }?.let {
                java.time.LocalDateTime.parse(it)
            } ?: opprettet

            val innsendtTid = søknadsdataPacket["innsendt"].asText().takeIf { it != "null" }?.let {
                java.time.LocalDateTime.parse(it)
            } ?: opprettet

            OrkestratorSoknad
                .newBuilder()
                .apply {
                    this.soknadId = søknadId
                    this.opprettet = opprettetTid.atZone(java.time.ZoneId.systemDefault()).toInstant()
                    this.innsendt = innsendtTid.atZone(java.time.ZoneId.systemDefault()).toInstant()
                    this.personalia = parseSeksjon(søknadsdataPacket["personalia"].asText())
                    this.dinSituasjon = parseSeksjon(søknadsdataPacket["din-situasjon"]?.asText())
                    this.arbeidsforhold = parseSeksjon(søknadsdataPacket["arbeidsforhold"]?.asText())
                    this.annenPengestotte = parseSeksjon(søknadsdataPacket["annen-pengestotte"]?.asText())
                    this.egenNaring = parseSeksjon(søknadsdataPacket["egen-naring"]?.asText())
                    this.verneplikt = parseSeksjon(søknadsdataPacket["verneplikt"]?.asText())
                    this.utdanning = parseSeksjon(søknadsdataPacket["utdanning"]?.asText())
                    this.barnetillegg = parseSeksjon(søknadsdataPacket["barnetillegg"]?.asText())
                    this.reellArbeidssoker = parseSeksjon(søknadsdataPacket["reell-arbeidssoker"]?.asText())
                    this.tilleggsopplysninger = parseSeksjon(søknadsdataPacket["tilleggsopplysninger"]?.asText())
                }.build()
                .also { data ->
                    sikkerlogg.info {"Publiserer søknadsdata for søknadId=$søknadId til topic ${dataTopic.topic}, data=${data}, på ${dataTopic.topic}" }
                    logger.info {"Publiserer søknadsdata for søknadId=$søknadId til topic ${dataTopic.topic}, data=${data}" }
                    dataTopic.publiser(søknadId.toString(), data)
                }
        }
    }

    private fun parseSeksjon(seksjon: String?): Seksjonsinfo? {
        if (seksjon.isNullOrBlank()) return null
        return try {
            val seksjonJson = objectMapper.readTree(seksjon)
            Seksjonsinfo.newBuilder()
                .setSeksjonId(seksjonJson["seksjonId"].asText())
                .setSeksjonsvar(
                    seksjonJson["seksjonsvar"]
                        ?.let { node ->
                            val map = mutableMapOf<String, String>()
                            node.properties().forEach { (key, value) ->
                                map[key] = if (value.isTextual) value.asText() else value.toString()
                            }
                            map
                        } ?: mutableMapOf(),
                )
                .setVersjon(seksjonJson["versjon"].asText())
                .build()
        } catch (e: Exception) {
            logger.warn(e) { "Kunne ikke parse seksjon: $seksjon" }
            null
        }
    }

}
