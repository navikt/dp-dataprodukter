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
import java.time.LocalDate

internal class BehandlingRiverTest {
    private val producer = mockk<KafkaProducer<String, Behandlingsresultat>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val rapid by lazy {
        TestRapid().apply {
            BehandlingRiver(
                rapidsConnection = this,
                dataTopic = dataTopic,
                datoViEierAvslag = LocalDate.MIN,
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
            this.foerteTil shouldBe "Avslag"
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
            this.foerteTil shouldBe "Gjenopptak"
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
            this.foerteTil shouldBe "Endring"
        }
    }

    @Test
    fun `kan videreformidle avslag på gjennopptak`() {
        rapid.sendTestMessage(avslagGjenopptak)

        val value = slot<ProducerRecord<String, Behandlingsresultat>>()
        verify {
            producer.send(capture(value), any())
        }

        value.isCaptured shouldBe true
        val key = value.captured.key()

        with(value.captured.value()) {
            this.ident shouldBe key
            this.foerteTil shouldBe "Avslag"
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

    private val avslagGjenopptak by lazy {
        // Henter fra sak i testmiljøet, behandlingid 019c2ca9-261f-74c2-92da-02cf743c06ef
        javaClass.getResource("/dp-behandling/behandlingsresultat_gjenopptak_avslag.json")!!.readText()
    }
}
