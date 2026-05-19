package no.nav.dagpenger.dataprodukter.produkter.utbetaling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.dataprodukt.utbetaling.Utbetaling
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

class UtbetalingRiverTest {
    private val producer = mockk<KafkaProducer<String, Utbetaling>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val rapid by lazy {
        TestRapid().apply {
            UtbetalingRiver(
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
    fun `skal poste utbetaling ut på Kafka`() {
        rapid.sendTestMessage(utbetalingJSON)

        val value = slot<ProducerRecord<String, Utbetaling>>()
        verify {
            producer.send(capture(value), any())
        }
        value.isCaptured shouldBe true
        value.captured.key() shouldBe "12345678901"

        with(value.captured.value()) {
            this.ident shouldBe "12345678901"
            utbetalingId shouldBe "Ej5FZ+ibEtOkVkJmFBdAAA=="
            behandlingId shouldBe UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
            sakId shouldBe UUID.fromString("123e4567-e89b-12d3-a456-426614174001")
            sakIdBase64 shouldBe "Ej5FZ+ibEtOkVkJmFBdAAQ=="
            behandletHendelseId shouldBe "m1"
            behandletHendelseType shouldBe "type_m1"
        }
    }

    @Language("JSON")
    private val utbetalingJSON =
        """
        {
          "@event_name": "utbetaling_utført",
          "ident": "12345678901",
          "behandlingId": "123e4567-e89b-12d3-a456-426614174000",
          "eksternBehandlingId": "Ej5FZ+ibEtOkVkJmFBdAAA==",
          "sakId": "123e4567-e89b-12d3-a456-426614174001",
          "eksternSakId": "Ej5FZ+ibEtOkVkJmFBdAAQ==",
          "behandletHendelseId": "m1",
          "behandletHendelseType": "type_m1",
          "meldekortId": "m1",
          "status": "FERDIG",
          "@id": "bb17016c-a381-4d49-b086-5ecefbb2c073",
          "@opprettet": "2026-05-19T12:49:30.559953",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "bb17016c-a381-4d49-b086-5ecefbb2c073",
              "time": "2026-05-19T12:49:30.559953"
            }
          ]
        }        
        """.trimIndent()
}
