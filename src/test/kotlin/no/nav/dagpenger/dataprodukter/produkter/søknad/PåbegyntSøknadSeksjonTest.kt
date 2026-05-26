package no.nav.dagpenger.dataprodukter.produkter.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.dataprodukt.soknad.OrkestratorSeksjon
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import no.nav.dagpenger.dataprodukter.person.PersonRepository
import no.nav.dagpenger.dataprodukter.person.PersonsBeskyttelseInfo
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

internal class PåbegyntSøknadSeksjonTest {
    private val producer = mockk<KafkaProducer<String, OrkestratorSeksjon>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val personRepository = mockk<PersonRepository>()
    private val rapid =
        TestRapid().also {
            PåbegyntSøknadSeksjon(it, dataTopic, personRepository)
        }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `Publiserer påbegynt søknad seksjon uten adressebeskyttelse`() {
        val søknadId = UUID.randomUUID()
        val seksjonId = "personalia"
        val opprettet = "2024-02-01T12:00:00"
        val oppdatert = "2024-02-01T12:01:00"
        val slot = mutableListOf<ProducerRecord<String, OrkestratorSeksjon>>()

        every {
            personRepository.hentPersonMedKode6Og7BeskyttelseInfo(any())
        } returns PersonsBeskyttelseInfo(harAdressebeskyttelse = false)
        every { producer.send(capture(slot), any()) } returns mockk()

        rapid.sendTestMessage(påbegyntSeksjonEvent(søknadId, seksjonId, opprettet, oppdatert))

        verify(exactly = 1) {
            producer.send(any(), any())
        }

        val record = slot.single()
        val seksjon = record.value()

        assertEquals(søknadId.toString(), record.key())
        assertEquals(seksjonId, seksjon["seksjonId"])
        assertEquals(søknadId, seksjon["soknadId"])
        assertEquals(opprettet.tilInstant(), seksjon["opprettet"])
        assertEquals(oppdatert.tilInstant(), seksjon["oppdatert"])
    }

    @Test
    fun `Publiserer ikke påbegynt søknad seksjon ved adressebeskyttelse`() {
        every {
            personRepository.hentPersonMedKode6Og7BeskyttelseInfo(any())
        } returns PersonsBeskyttelseInfo(harAdressebeskyttelse = true)

        rapid.sendTestMessage(
            påbegyntSeksjonEvent(
                søknadId = UUID.randomUUID(),
                seksjonId = "personalia",
                opprettet = "2024-02-01T12:00:00",
                oppdatert = "2024-02-01T12:01:00",
            ),
        )

        verify(exactly = 0) {
            producer.send(any(), any())
        }
    }
}

private fun påbegyntSeksjonEvent(
    søknadId: UUID,
    seksjonId: String,
    opprettet: String,
    oppdatert: String,
) =
    JsonMessage
        .newMessage(
            "paabegynt_soknad_seksjon",
            mapOf(
                "søknad_uuid" to søknadId.toString(),
                "ident" to "12345678901",
                "seksjon_id" to seksjonId,
                "opprettet" to opprettet,
                "oppdatert" to oppdatert,
            ),
        ).toJson()

private fun String.tilInstant() =
    LocalDateTime
        .parse(this)
        .atZone(ZoneId.of("Europe/Oslo"))
        .toInstant()
