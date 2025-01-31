package no.nav.dagpenger.dataprodukter.produkter

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.dataprodukt.soknad.Dokumentkrav
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import no.nav.dagpenger.dataprodukter.person.Person
import no.nav.dagpenger.dataprodukter.person.PersonRepository
import no.nav.dagpenger.dataprodukter.produkter.søknad.DokumentkravRiver
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class DokumentkravRiverTest {
    private val producer = mockk<KafkaProducer<String, Dokumentkrav>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val personRepository = mockk<PersonRepository>()
    private val rapid =
        TestRapid().also {
            DokumentkravRiver(it, dataTopic)
        }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `Innsendt dokumentkrav blir publisert`() {
        every {
            personRepository.hentPerson(any())
        } returns Person(harAdressebeskyttelse = false)

        rapid.sendTestMessage(dokumentkravEvent)

        verify(exactly = 2) {
            producer.send(any(), any())
        }
    }

    @Test
    fun `Innsendt dokumentkrav blir ikke publisert når adressebeskyttelse`() {
        every {
            personRepository.hentPerson(any())
        } returns Person(harAdressebeskyttelse = true)

        rapid.sendTestMessage(dokumentkravEvent)

        verify(exactly = 0) {
            producer.send(any(), any())
        }
    }
}

private val dokumentkravEvent =
    JsonMessage
        .newMessage(
            "dokumentkrav_innsendt",
            mapOf(
                "søknad_uuid" to UUID.randomUUID(),
                "ident" to "123123",
                "søknadType" to "Dagpenger",
                "innsendingsType" to "Ettersending",
                "innsendttidspunkt" to LocalDateTime.now(),
                "hendelseId" to UUID.randomUUID(),
                "dokumentkrav" to
                    listOf(
                        mapOf(
                            "dokumentnavn" to "Dokumentnavn",
                            "skjemakode" to "O2",
                            "valg" to "Sendes nå",
                        ),
                        mapOf(
                            "dokumentnavn" to "Dokumentnavn2",
                            "skjemakode" to "O2",
                            "valg" to "Sendes nå",
                        ),
                    ),
            ),
        ).toJson()
