package no.nav.dagpenger.dataprodukter.produkter

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.dataprodukt.soknad.SoknadTilstand
import no.nav.dagpenger.dataprodukter.helpers.tilstandEndretEvent
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import no.nav.dagpenger.dataprodukter.produkter.søknad.SøknadTilstandRiver
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class SøknadTilstandRiverTest {
    private val producer = mockk<KafkaProducer<String, SoknadTilstand>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val rapid =
        TestRapid().also {
            SøknadTilstandRiver(it, dataTopic)
        }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `Hver endring i tilstand blir publisert`() {
        val søknadId = UUID.randomUUID()
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "123123", "Opprettet"))
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "123123", "Innsendt"))
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "123123", "Journalført"))
        rapid.sendTestMessage(tilstandEndretEvent(søknadId, "123123", "Slettet"))

        verify(exactly = 4) {
            producer.send(any(), any())
        }
    }
}
