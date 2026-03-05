package no.nav.dagpenger.dataprodukter.produkter.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.dataprodukt.soknad.OrkestratorSoknad
import no.nav.dagpenger.dataprodukt.soknad.SoknadFaktum
import no.nav.dagpenger.dataprodukter.helpers.Seksjoner
import no.nav.dagpenger.dataprodukter.helpers.faktum
import no.nav.dagpenger.dataprodukter.helpers.generator
import no.nav.dagpenger.dataprodukter.helpers.seksjon
import no.nav.dagpenger.dataprodukter.helpers.tilstandEndretEvent
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import no.nav.dagpenger.dataprodukter.objectMapper
import no.nav.dagpenger.dataprodukter.person.Person
import no.nav.dagpenger.dataprodukter.person.PersonRepository
import no.nav.dagpenger.dataprodukter.søknad.InMemorySøknadRepository
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

internal class SøknadsdataRiverTest {
    private val repository = InMemorySøknadRepository()
    private val producer = mockk<KafkaProducer<String, SoknadFaktum>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val personRepository = mockk<PersonRepository>()
    private val rapid =
        TestRapid().also {
            SøknadsdataRiver(it, repository, personRepository)
            SøknadInnsendtRiver(it, repository, dataTopic, listOf("sperret-faktum"))
        }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `Søknadfakta blir mellomlagret`() {
        every {
            personRepository.hentPerson(any())
        } returns Person(harAdressebeskyttelse = false)

        val søknadId = UUID.randomUUID()
        rapid.sendTestMessage(getSøknadData(søknadId))
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "123123", "Innsendt"))

        verify(exactly = 9) {
            producer.send(any(), any())
        }
    }

    @Test
    fun `Søknadfakta blir ikke mellomlagret når adressebeskyttelse`() {
        every {
            personRepository.hentPerson(any())
        } returns Person(harAdressebeskyttelse = true)

        val søknadId = UUID.randomUUID()
        rapid.sendTestMessage(getSøknadData(søknadId))
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "123123", "Innsendt"))

        verify(exactly = 0) {
            producer.send(any(), any())
        }
    }
}

private fun getSøknadData(søknadId: UUID) =
    getDataMessage(søknadId) {
        seksjon(
            faktum("seksjon1-faktum1", "land", "NOR"),
            faktum("seksjon1-faktum2", "land", "NOR"),
            faktum("sperret-faktum", "date", "2022-02-02"),
        )
        seksjon(faktum("seksjon2-faktum1", "land", "NOR"))
        seksjon(
            generator(
                "arbeidsforhold",
                faktum("arbeidsforhold1-faktum1", "int", 123, "1.1"),
                faktum("arbeidsforhold1-faktum2", "int", 345, "2.1"),
            ),
            generator(
                "barn",
                faktum("barn1-faktum1", "string", "Per", "3.1"),
                faktum("barn1-faktum2", "bool", true, "4.1"),
                faktum("barn2-faktum1", "string", "Arne", "3.2"),
                faktum("barn2-faktum2", "bool", false, "4.2"),
            ),
        )
    }

internal class OrkestratorSøknadsdataRiverTest {
    private val producer = mockk<KafkaProducer<String, OrkestratorSoknad>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "orkestrator-søknadsdata")
    private val rapid =
        TestRapid().also {
            OrkestratorSøknadsdataRiver(it, dataTopic)
        }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `Mottar søknadsdata fra orkestrator og publiserer til dataTopic`() {
        val søknadId = UUID.randomUUID()
        rapid.sendTestMessage(getOrkestratorSøknadEvent(søknadId))

        verify(exactly = 1) {
            producer.send(any(), any())
        }
    }

    @Test
    fun `Ignorerer søknad_endret_tilstand uten kilde orkestrator`() {
        val søknadId = UUID.randomUUID()
        rapid.sendTestMessage(getSøknadEndretTilstandUtenKilde(søknadId))

        verify(exactly = 0) {
            producer.send(any(), any())
        }
    }

    @Test
    fun `Ignorerer søknad_endret_tilstand med annen tilstand enn Innsendt`() {
        val søknadId = UUID.randomUUID()
        rapid.sendTestMessage(getOrkestratorSøknadEvent(søknadId, gjeldendeTilstand = "Påbegynt"))

        verify(exactly = 0) {
            producer.send(any(), any())
        }
    }
}

private fun getOrkestratorSøknadEvent(
    søknadId: UUID,
    gjeldendeTilstand: String = "Innsendt",
): String {
    val søknadsdata = mapOf(
        "søknad_uuid" to søknadId.toString(),
        "opprettet" to Instant.now().toEpochMilli(),
        "innsendt" to Instant.now().toEpochMilli(),
        "personalia" to mapOf(
            "seksjonId" to "personalia",
            "seksjonsvar" to "{}",
            "versjon" to "1.0",
        ),
        "dinSituasjon" to null,
        "arbeidsforhold" to null,
        "annenPengestotte" to null,
        "egenNaring" to null,
        "verneplikt" to null,
        "utdanning" to null,
        "barnetillegg" to null,
        "reellArbeidssoker" to null,
        "tilleggsopplysninger" to null,
    )

    return JsonMessage
        .newMessage(
            "søknad_endret_tilstand",
            mapOf(
                "søknad_uuid" to søknadId.toString(),
                "søknadId" to søknadId.toString(),
                "@opprettet" to "2024-01-01T12:00:00",
                "gjeldendeTilstand" to gjeldendeTilstand,
                "kilde" to "orkestrator",
                "søknadsdata" to objectMapper.writeValueAsString(søknadsdata),
            ),
        ).toJson()
}

private fun getSøknadEndretTilstandUtenKilde(søknadId: UUID) =
    JsonMessage
        .newMessage(
            "søknad_endret_tilstand",
            mapOf(
                "søknad_uuid" to søknadId,
                "@opprettet" to "2024-01-01T12:00:00",
                "gjeldendeTilstand" to "Innsendt",
            ),
        ).toJson()

private fun getDataMessage(
    uuid: UUID,
    seksjoner: Seksjoner.() -> Seksjoner,
) = JsonMessage
    .newMessage(
        "søker_oppgave",
        mapOf(
            "versjon_navn" to "Dagpenger",
            "søknad_uuid" to uuid,
            "ferdig" to true,
            "seksjoner" to seksjoner(mutableListOf()),
            "fødselsnummer" to "123123",
        ),
    ).toJson()
