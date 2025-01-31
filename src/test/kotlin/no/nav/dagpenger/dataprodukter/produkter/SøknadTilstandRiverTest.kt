package no.nav.dagpenger.dataprodukter.produkter

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.dataprodukt.soknad.SoknadTilstand
import no.nav.dagpenger.dataprodukter.helpers.tilstandEndretEvent
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import no.nav.dagpenger.dataprodukter.person.Person
import no.nav.dagpenger.dataprodukter.person.PersonRepository
import no.nav.dagpenger.dataprodukter.produkter.søknad.SøknadTilstandRiver
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class SøknadTilstandRiverTest {
    private val producer = mockk<KafkaProducer<String, SoknadTilstand>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val personRepository = mockk<PersonRepository>()
    private val rapid =
        TestRapid().also {
            SøknadTilstandRiver(it, dataTopic, personRepository)
        }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `Hver endring i tilstand blir publisert`() {
        every {
            personRepository.hentPerson(any())
        } returns Person(harAdressebeskyttelse = false)

        val søknadId = UUID.randomUUID()
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "123123", "Opprettet"))
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "123123", "Innsendt"))
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "123123", "Journalført"))
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "123123", "Slettet"))

        verify(exactly = 4) {
            producer.send(any(), any())
        }
    }

    @Test
    fun `Endring i tilstand blir ikke publisert når adressebeskyttelse`() {
        every {
            personRepository.hentPerson(any())
        } returns Person(harAdressebeskyttelse = true)

        val søknadId = UUID.randomUUID()
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "123123", "Opprettet"))
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "123123", "Innsendt"))
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "123123", "Journalført"))
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "123123", "Slettet"))

        verify(exactly = 0) {
            producer.send(any(), any())
        }
    }
}
