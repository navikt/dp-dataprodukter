package no.nav.dagpenger.data.innlop.tjenester

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.data.innlop.SoknadTilstand
import no.nav.dagpenger.data.innlop.helpers.tilstandEndretEvent
import no.nav.dagpenger.data.innlop.kafka.DataTopic
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class SøknadTilstandRiverTest {
    private val producer = mockk<KafkaProducer<String, SoknadTilstand>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val rapid = TestRapid().also {
        SøknadTilstandRiver(it, dataTopic)
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `Hver endring i tilstand blir publisert`() {
        val søknadId = UUID.randomUUID()
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "Opprettet"))
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "Innsendt"))
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "Journalført"))
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "Slettet"))

        verify(exactly = 4) {
            producer.send(any(), any())
        }
    }
}
