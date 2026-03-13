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
import no.nav.dagpenger.dataprodukter.person.Person
import no.nav.dagpenger.dataprodukter.person.PersonRepository
import no.nav.dagpenger.dataprodukter.søknad.InMemorySøknadRepository
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
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

    @Test
    fun `Arbeidsforhold i seksjonsvar er serialisert riktig som JSON string`() {
        val søknadId = UUID.randomUUID()
        val slot = mutableListOf<org.apache.kafka.clients.producer.ProducerRecord<String, OrkestratorSoknad>>()
        every { producer.send(capture(slot), any()) } returns mockk()

        rapid.sendTestMessage(getOrkestratorSøknadEventWithNestedArbeidsforhold(søknadId))

        verify(exactly = 1) {
            producer.send(any(), any())
        }

        val capturedRecord = slot.first()
        val arbeidsforhold = capturedRecord.value().arbeidsforhold
        assert(arbeidsforhold != null) { "Arbeidsforhold should not be null" }
        val registrerteArbeidsforhold = arbeidsforhold!!.seksjonsvar["registrerteArbeidsforhold"]
        assert(registrerteArbeidsforhold != null) { "registrerteArbeidsforhold should not be null" }
        assert(registrerteArbeidsforhold!!.contains("Jobb AS"))
        assert(registrerteArbeidsforhold.startsWith("["))
    }
}


private fun getOrkestratorSøknadEvent(
    søknadId: UUID,
    gjeldendeTilstand: String = "Innsendt",
): String {
    val søknadsdata = mapOf(
        "opprettet" to "2024-01-01T12:00:00",
        "innsendt" to "2024-01-01T12:30:00",
        "personalia" to """{"seksjonId":"personalia","seksjonsvar":{"fornavnFraPdl":"FIRKANTET","etternavnFraPdl":"JEGER","alderFraPdl":"54","poststedFraPdl":"Dilling","landkodeFraPdl":"NO","landFraPdl":"NORGE","kontonummerFraKontoregister":"","folkeregistrertAdresseErNorgeStemmerDet":"ja"},"versjon":1}""",
        "din-situasjon" to """{"seksjonId":"din-situasjon","seksjonsvar":{"harDuMottattDagpengerFraNavILøpetAvDeSiste52Ukene":"vetikke","hvilkenDatoSøkerDuDagpengerFra":"2026-03-03"},"versjon":1}""",
        "arbeidsforhold" to """{"seksjonId":"arbeidsforhold","seksjonsvar":{"hvordanHarDuJobbet":"harIkkeJobbetDeSiste36Månedene","registrerteArbeidsforhold":[]},"versjon":1}""",
        "annen-pengestotte" to """{"seksjonId":"annen-pengestotte","seksjonsvar":{"harMottattEllerSøktOmPengestøtteFraAndreEøsLand":"nei","pengestøtteFraAndreEøsLand":[],"mottarDuAndreUtbetalingerEllerØkonomiskeGoderFraTidligereArbeidsgiver":"nei","pengestøtteFraTidligereArbeidsgiver":[],"mottarDuPengestøtteFraAndreEnnNav":"nei","pengestøtteFraNorge":[]},"versjon":1}""",
        "egen-naring" to """{"seksjonId":"egen-naring","seksjonsvar":{"driverDuEgenNæringsvirksomhet":"nei","næringsvirksomheter":null,"driverDuEgetGårdsbruk":"nei","gårdsbruk":null},"versjon":1}""",
        "verneplikt" to """{"seksjonId":"verneplikt","seksjonsvar":{"avtjentVerneplikt":"nei","dokumentasjonskrav":"null"},"versjon":1}""",
        "utdanning" to """{"seksjonId":"utdanning","seksjonsvar":{"tarUtdanningEllerOpplæring":"ja","dokumentasjonskrav":"null"},"versjon":1}""",
        "barnetillegg" to """{"seksjonId":"barnetillegg","versjon":1,"seksjonsvar":{"barnFraPdl":null,"forsørgerDuBarnSomIkkeVisesHer":"nei","barnLagtManuelt":null}}""",
        "reell-arbeidssoker" to """{"seksjonId":"reell-arbeidssoker","seksjonsvar":{"kanDuJobbeBådeHeltidOgDeltid":"ja","kanDuJobbeIHeleNorge":"ja","kanDuTaAlleTyperArbeid":"ja","erDuVilligTilÅBytteYrkeEllerGåNedILønn":"ja","dokumentasjonskrav":"null"},"versjon":1}""",
        "tilleggsopplysninger" to """{"seksjonId":"tilleggsopplysninger","seksjonsvar":{"harTilleggsopplysninger":"nei"},"versjon":1}""",
    )

    return JsonMessage
        .newMessage(
            "søknad_endret_tilstand",
            mapOf(
                "søknad_uuid" to søknadId.toString(),
                "ident" to "12345678901",
                "forrigeTilstand" to "Påbegynt",
                "gjeldendeTilstand" to gjeldendeTilstand,
                "kilde" to "orkestrator",
                "@opprettet" to "2024-01-01T12:00:00",
                "søknadsdata" to søknadsdata,
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

private fun getOrkestratorSøknadEventWithNestedArbeidsforhold(søknadId: UUID): String {
    val søknadsdata = mapOf(
        "opprettet" to "2024-01-01T12:00:00",
        "innsendt" to "2024-01-01T12:30:00",
        "personalia" to """{"seksjonId":"personalia","seksjonsvar":{"fornavnFraPdl":"FIRKANTET"},"versjon":"1"}""",
        "arbeidsforhold" to """{"seksjonId":"arbeidsforhold","seksjonsvar":{"hvordanHarDuJobbet":"fastArbeidstidIMindreEnn6Måneder","harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene":"nei","registrerteArbeidsforhold":[{"navnetPåBedriften":"Jobb AS","hvilketLandJobbetDuI":"NOR","hvordanHarDetteArbeidsforholdetEndretSeg":"jegErPermitert","id":"d368ee1c-427d-4885-ada7-7aa792e4a52a"}]},"versjon":"1"}""",
    )

    return JsonMessage
        .newMessage(
            "søknad_endret_tilstand",
            mapOf(
                "søknad_uuid" to søknadId.toString(),
                "ident" to "12345678901",
                "forrigeTilstand" to "Påbegynt",
                "gjeldendeTilstand" to "Innsendt",
                "kilde" to "orkestrator",
                "@opprettet" to "2024-01-01T12:00:00",
                "søknadsdata" to søknadsdata,
            ),
        ).toJson()
}

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
