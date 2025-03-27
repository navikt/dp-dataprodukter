package no.nav.dagpenger.dataprodukter.produkter.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.dataprodukt.behandling.Behandling
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class VedtakRiverTest {
    private val producer = mockk<KafkaProducer<String, Behandling>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val rapid by lazy {
        TestRapid().apply {
            VedtakRiver(
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
        rapid.sendTestMessage(innvilgelsesVedtak)

        verify {
            producer.send(any(), any())
        }
    }

    private val innvilgelsesVedtak by lazy {
        // Generert i dp-behandling: https://github.com/navikt/dp-behandling/blob/459cbfe6e41362be45133ff2ca52d4a56ad2d1bb/mediator/src/test/kotlin/no/nav/dagpenger/behandling/PersonMediatorTest.kt#L323
        javaClass.getResource("/dp-behandling/vedtak_fattet_innvilgelse.json")!!.readText()
    }
}
