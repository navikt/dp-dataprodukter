package no.nav.dagpenger.dataprodukter.produkter.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.dataprodukt.behandling.BehandlingEndretTilstand
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BehandlingEndretTilstandRiverTest {
    private val producer = mockk<KafkaProducer<String, BehandlingEndretTilstand>>(relaxed = true)
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
    fun `skal poste forslag eller fattet vedtak ut på Kafka`() {
        rapid.sendTestMessage(behandlingEndretTilstandJSON)

        val value = slot<ProducerRecord<String, BehandlingEndretTilstand>>()
        verify {
            producer.send(capture(value), any())
        }

        value.isCaptured shouldBe true

        value.captured.key() shouldBe "11109233444"

        with(value.captured.value()) {
            this.ident shouldBe "11109233444"
            forrigeTilstand shouldBe "UnderOpprettelse"
            gjeldendeTilstand shouldBe "UnderBehandling"
            forventetFerdig.shouldBeNull()
            tidBruktSekund shouldBe 8640000L
            tidBrukt shouldBe "PT2400H"
        }
    }

    private val behandlingEndretTilstandJSON =
        """
        {
          "@event_name": "behandling_endret_tilstand",
          "ident": "11109233444",
          "behandlingId": "01962470-6e8b-7f6d-9e9b-c722c1fbc1a1",
          "forrigeTilstand": "UnderOpprettelse",
          "gjeldendeTilstand": "UnderBehandling",
          "forventetFerdig": "${LocalDateTime.MAX}",
          "tidBrukt": "PT2400H",
          "@id": "1bf1426d-3092-409e-9f8e-d88ab453cbde",
          "@opprettet": "2025-04-11T12:41:10.29518",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "1bf1426d-3092-409e-9f8e-d88ab453cbde",
              "time": "2025-04-11T12:41:10.295180"
            }
          ],
          "@forårsaket_av": {
            "id": "55f26414-5872-4836-bf7e-2666e5de8114",
            "opprettet": "2025-04-11T12:41:10.244472",
            "event_name": "søknad_behandlingsklar"
          }
        }
        """.trimIndent()
}
