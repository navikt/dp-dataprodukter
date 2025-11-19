package no.nav.dagpenger.dataprodukter.produkter.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.dataprodukt.behandling.Behandlingsresultat
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class BehandlingRiverTest {
    private val producer = mockk<KafkaProducer<String, Behandlingsresultat>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val rapid by lazy {
        TestRapid().apply {
            BehandlingRiver(
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
    fun `kan videreformidle avslag`() {
        rapid.sendTestMessage(avslag)

        val value = slot<ProducerRecord<String, Behandlingsresultat>>()
        verify {
            producer.send(capture(value), any())
        }

        value.isCaptured shouldBe true
        val key = value.captured.key()

        with(value.captured.value()) {
            this.ident shouldBe key
            this.resultat shouldBe "Avslag"
        }
    }

    @Test
    fun `kan videreformidle innvilgelse`() {
        rapid.sendTestMessage(innvilgelse)

        val value = slot<ProducerRecord<String, Behandlingsresultat>>()
        verify {
            producer.send(capture(value), any())
        }

        value.isCaptured shouldBe true
        val key = value.captured.key()

        with(value.captured.value()) {
            this.ident shouldBe key
            this.resultat shouldBe "Gjenopptak"
        }
    }

    @Test
    fun `kan videreformidle beregning`() {
        rapid.sendTestMessage(beregning)

        val value = slot<ProducerRecord<String, Behandlingsresultat>>()
        verify {
            producer.send(capture(value), any())
        }

        value.isCaptured shouldBe true
        val key = value.captured.key()

        with(value.captured.value()) {
            this.ident shouldBe key
            this.resultat shouldBe "Beregning"
        }
    }

    private val avslag by lazy {
        // Generert i dp-behandling: https://github.com/navikt/dp-behandling/blob/459cbfe6e41362be45133ff2ca52d4a56ad2d1bb/mediator/src/test/kotlin/no/nav/dagpenger/behandling/PersonMediatorTest.kt#L323
        javaClass.getResource("/dp-behandling/behandlingsresultat_avslag.json")!!.readText()
    }

    private val innvilgelse by lazy {
        // Generert i dp-behandling: https://github.com/navikt/dp-behandling/blob/459cbfe6e41362be45133ff2ca52d4a56ad2d1bb/mediator/src/test/kotlin/no/nav/dagpenger/behandling/PersonMediatorTest.kt#L323
        javaClass.getResource("/dp-behandling/behandlingsresultat_gjenopptak.json")!!.readText()
    }
    private val beregning by lazy {
        // Generert i dp-behandling: https://github.com/navikt/dp-behandling/blob/459cbfe6e41362be45133ff2ca52d4a56ad2d1bb/mediator/src/test/kotlin/no/nav/dagpenger/behandling/PersonMediatorTest.kt#L323
        javaClass.getResource("/dp-behandling/behandlingsresultat_beregnet.json")!!.readText()
    }
}
