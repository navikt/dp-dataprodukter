package no.nav.dagpenger.dataprodukter.produkter

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.dataprodukt.behandling.Behandling
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import no.nav.dagpenger.dataprodukter.produkter.behandling.BehandlingEndretTilstandRiver
import org.apache.kafka.clients.producer.KafkaProducer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class BehandlingEndretTilstandRiverTest {
    private val producer = mockk<KafkaProducer<String, Behandling>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val rapid by lazy {
        TestRapid().apply {
            BehandlingEndretTilstandRiver(
                rapidsConnection = this,
                dataTopic = dataTopic,
            )
        }
    }

    init {
        System.setProperty("kafka_produkt_topic", "foobar")
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `skal poste inntekt ut på Kafka`() {
        rapid.sendTestMessage(tilstandEndretEvent)

        verify {
            producer.send(any(), any())
        }
    }

    @Language("JSON")
    private val tilstandEndretEvent =
        """{
        |  "@event_name": "behandling_endret_tilstand",
        |  "ident": "11109233444",
        |  "behandlingId": "019078d2-a873-74b4-95f2-0a66dbe59afd",
        |  "forrigeTilstand": "UnderOpprettelse",
        |  "gjeldendeTilstand": "UnderBehandling",
        |  "forventetFerdig": "2024-07-03T16:39:50.006487",
        |  "tidBrukt": "PT2.661S",
        |  "@id": "013fbdec-07cb-438c-acb1-573f92c9919f",
        |  "@opprettet": "2024-07-03T15:39:50.007145",
        |  "system_read_count": 0,
        |  "system_participating_services": [
        |    {
        |      "id": "013fbdec-07cb-438c-acb1-573f92c9919f",
        |      "time": "2024-07-03T15:39:50.007145",
        |      "image": "image:sha"
        |    }
        |  ]
        |}
        """.trimMargin()
}
