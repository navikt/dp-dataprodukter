package no.nav.dagpenger.dataprodukter.produkter.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import java.time.LocalDateTime
import no.nav.dagpenger.dataprodukt.soknad.OrkestratorSeksjon
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
    private val seksjonDataTopic: DataTopic<OrkestratorSeksjon>,
    private val søknadDataTopic: DataTopic<OrkestratorSoknad>,
    private val personRepository: PersonRepository,
    ) : River.PacketListener {
    private val seksjoner = setOf(
        "personalia",
        "din-situasjon",
        "arbeidsforhold",
        "annen-pengestotte",
        "egen-naring",
        "verneplikt",
        "utdanning",
        "barnetillegg",
        "reell-arbeidssoker",
        "tilleggsopplysninger",
    )

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "søknad_endret_tilstand") }
                precondition { it.requireValue("gjeldendeTilstand", "Innsendt") }
                precondition { it.requireValue("kilde", "orkestrator") }
                validate { it.requireKey("søknad_uuid", "@opprettet", "søknadsdata", "ident") }
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
        val ident = packet["ident"].asText()

        val person = personRepository.hentPersonMedKode6Og7BeskyttelseInfo(ident)

        if (person.harAdressebeskyttelse) return

        val søknadsdataPacket = packet["søknadsdata"]
        withLoggingContext(
            "søknadId" to søknadId.toString(),
            "dataprodukt" to "orkestrator-søknadsdata",
        ) {
            logger.info { "Mottok innsendt søknad fra orkestrator, søknadId=$søknadId opprettet=$opprettet" }

            søknadsdataPacket.properties().forEach { (key, value) ->
                if(key in seksjoner) {
                    val seksjonsdata = objectMapper.readTree(value["seksjonsdata"].asText())
                    OrkestratorSeksjon.newBuilder().apply {
                        this.soknadId = søknadId
                        this.seksjonId = key
                        this.opprettet = value["opprettet"].asText().takeIf { it != "null" }?.let {
                            LocalDateTime.parse(it).atZone(java.time.ZoneId.systemDefault()).toInstant()
                        }
                        this.oppdatert = value["oppdatert"].asText().takeIf { it != "null" }?.let {
                            LocalDateTime.parse(it).atZone(java.time.ZoneId.systemDefault()).toInstant()
                        } ?: this.opprettet
                        this.seksjonsvar = mapSeksjonssvar(seksjonsdata["seksjonsvar"])
                        this.versjon = seksjonsdata["versjon"].asText()
                     }.build().also { data ->
                        logger.info { "Publiserer seksjonsdata for søknadId=$søknadId, seksjonId=$key til topic ${seksjonDataTopic.topic}" }
                        sikkerlogg.info { "Publiserer seksjonsdata for søknadId=$søknadId, seksjonId=$key til topic ${seksjonDataTopic.topic}, data=${data}" }
                        seksjonDataTopic.publiser(søknadId.toString(), data)
                    }
                }
            }

            val opprettetTid = søknadsdataPacket["opprettet"].asText().takeIf { it != "null" }?.let {
                LocalDateTime.parse(it)
            } ?: opprettet

            val innsendtTid = søknadsdataPacket["innsendt"].asText().takeIf { it != "null" }?.let {
                LocalDateTime.parse(it)
            } ?: opprettet

            OrkestratorSoknad
                .newBuilder()
                .apply {
                    this.soknadId = søknadId
                    this.opprettet = opprettetTid.atZone(java.time.ZoneId.systemDefault()).toInstant()
                    this.innsendt = innsendtTid.atZone(java.time.ZoneId.systemDefault()).toInstant()
                    this.personalia = parseSeksjon(søknadsdataPacket["personalia"])
                    this.dinSituasjon = parseSeksjon(søknadsdataPacket["din-situasjon"])
                    this.arbeidsforhold = parseSeksjon(søknadsdataPacket["arbeidsforhold"])
                    this.annenPengestotte = parseSeksjon(søknadsdataPacket["annen-pengestotte"])
                    this.egenNaring = parseSeksjon(søknadsdataPacket["egen-naring"])
                    this.verneplikt = parseSeksjon(søknadsdataPacket["verneplikt"])
                    this.utdanning = parseSeksjon(søknadsdataPacket["utdanning"])
                    this.barnetillegg = parseSeksjon(søknadsdataPacket["barnetillegg"])
                    this.reellArbeidssoker = parseSeksjon(søknadsdataPacket["reell-arbeidssoker"])
                    this.tilleggsopplysninger = parseSeksjon(søknadsdataPacket["tilleggsopplysninger"])
                }.build()
                .also { data ->
                    sikkerlogg.info {"Publiserer søknadsdata for søknadId=$søknadId til topic ${seksjonDataTopic.topic}, data=${data}, på ${seksjonDataTopic.topic}" }
                    logger.info {"Publiserer søknadsdata for søknadId=$søknadId til topic ${seksjonDataTopic.topic}, data=${data}" }
                    søknadDataTopic.publiser(søknadId.toString(), data)
                }


        }
    }


    private fun parseSeksjon(seksjon: JsonNode?): Seksjonsinfo? {
        if (seksjon == null) return null
        return try {
            val seksjonsdata = objectMapper.readTree(seksjon["seksjonsdata"].asText())
            Seksjonsinfo.newBuilder()
                .setSeksjonId(seksjonsdata["seksjonId"].asText())
                .setSeksjonsvar(
                    seksjonsdata["seksjonsvar"]
                        ?.let { node ->
                            mapSeksjonssvar(node)
                        } ?: mutableMapOf(),
                )
                .setVersjon(seksjonsdata["versjon"].asText())
                .build()
        } catch (e: Exception) {
            null
        }
    }

    fun mapSeksjonssvar(seksjonsvar: JsonNode): Map<String, String>{
        val seksjonMap = mutableMapOf<String, String>()
        seksjonsvar.properties().forEach { (key, value) ->
            seksjonMap[key] = if (value.isTextual) value.asText() else value.toString()
        }
        return seksjonMap
    }
}
