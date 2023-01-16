package no.nav.dagpenger.data.innlop.tjenester

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.data.innlop.Dokumentkrav
import no.nav.dagpenger.data.innlop.kafka.DataTopic
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class DokumentkravRiverTest {
    private val producer = mockk<KafkaProducer<String, Dokumentkrav>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val rapid = TestRapid().also {
        DokumentkravRiver(it, dataTopic)
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `Innsendt dokumentkrav blir publisert`() {
        rapid.sendTestMessage(dokumentkravEvent)

        verify(exactly = 2) {
            producer.send(any(), any())
        }
    }
}

private val dokumentkravEvent = JsonMessage.newMessage(
    "dokumentkrav_innsendt",
    mapOf(
        "søknad_uuid" to UUID.randomUUID(),
        "ident" to "123123",
        "søknadType" to "Dagpenger",
        "innsendingsType" to "Ettersending",
        "innsendttidspunkt" to LocalDateTime.now(),
        "ferdigBesvart" to true,
        "hendelseId" to UUID.randomUUID(),
        "dokumentkrav" to listOf(
            mapOf(
                "dokumentnavn" to "Dokumentnavn",
                "skjemakode" to "O2",
                "valg" to "Sendes nå"
            ),
            mapOf(
                "dokumentnavn" to "Dokumentnavn2",
                "skjemakode" to "O2",
                "valg" to "Sendes nå"
            )
        )
    )
).toJson()
