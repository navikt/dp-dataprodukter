package no.nav.dagpenger.data.innlop.tjenester

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.data.innlop.DataTopic
import no.nav.dagpenger.data.innlop.SoknadFaktum
import no.nav.dagpenger.data.innlop.helpers.Seksjoner
import no.nav.dagpenger.data.innlop.helpers.faktum
import no.nav.dagpenger.data.innlop.helpers.generator
import no.nav.dagpenger.data.innlop.helpers.seksjon
import no.nav.dagpenger.data.innlop.søknad.InMemorySøknadRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class SøknadsdataRiverTest {
    private val repository = InMemorySøknadRepository()
    private val producer = mockk<KafkaProducer<String, SoknadFaktum>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val rapid = TestRapid().also {
        SøknadsdataRiver(it, repository)
        SøknadInnsendtRiver(it, repository, dataTopic)
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun foo() {
        val søknadId = UUID.randomUUID()
        rapid.sendTestMessage(getSøknadData(søknadId))
        rapid.sendTestMessage(getInnsendtMessage(søknadId))

        verify(exactly = 9) {
            producer.send(any())
        }
    }
}

private fun getSøknadData(søknadId: UUID) =
    getDataMessage(søknadId) {
        seksjon(
            faktum("seksjon1-faktum1", "land", "NOR"),
            faktum("seksjon1-faktum2", "land", "NOR")
        )
        seksjon(faktum("seksjon2-faktum1", "land", "NOR"))
        seksjon(
            generator(
                "arbeidsforhold",
                faktum("arbeidsforhold1-faktum1", "int", 123, "1.1"),
                faktum("arbeidsforhold1-faktum2", "int", 345, "2.1")
            ),
            generator(
                "barn",
                faktum("barn1-faktum1", "string", "Per", "3.1"),
                faktum("barn1-faktum2", "bool", true, "4.1"),
                faktum("barn2-faktum1", "string", "Arne", "3.2"),
                faktum("barn2-faktum2", "bool", false, "4.2")
            )
        )
    }

private fun getDataMessage(uuid: UUID, seksjoner: Seksjoner.() -> Seksjoner) =
    JsonMessage.newMessage(
        "søker_oppgave",
        mapOf(
            "versjon_navn" to "Dagpenger",
            "søknad_uuid" to uuid,
            "ferdig" to true,
            "seksjoner" to seksjoner(mutableListOf())
        )
    ).toJson()

private fun getInnsendtMessage(uuid: UUID) = JsonMessage.newMessage(
    "søknad_endret_tilstand",
    mapOf(
        "søknad_uuid" to uuid,
        "gjeldendeTilstand" to "Innsendt"
    )
).toJson()
