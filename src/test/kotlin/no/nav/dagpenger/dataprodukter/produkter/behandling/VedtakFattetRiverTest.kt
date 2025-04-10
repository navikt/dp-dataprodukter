package no.nav.dagpenger.dataprodukter.produkter.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.dataprodukt.behandling.Vedtak
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class VedtakFattetRiverTest {
    private val producer = mockk<KafkaProducer<String, Vedtak>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val rapid by lazy {
        TestRapid().apply {
            VedtakFattetRiver(
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
    fun `skal lage produkt av vedtak fattet`() {
        println(innvilgelsesVedtak)
        rapid.sendTestMessage(innvilgelsesVedtak)

        val value = slot<ProducerRecord<String, Vedtak>>()
        verify {
            producer.send(capture(value), any())
        }

        value.isCaptured shouldBe true
        value.captured.key() shouldBe "11109233444"

        with(value.captured.value()) {
            this.ident shouldBe "11109233444"
            this.vilkaar shouldHaveSize 18
            this.fastsatt.utfall shouldBe true

            this.fastsatt.grunnlag.grunnlag shouldBe 524904
            this.fastsatt.grunnlag.begrunnelse
                .shouldBeNull()

            this.fastsatt.fastsattVanligArbeidstid.vanligArbeidstidPerUke shouldBe 37.5
            this.fastsatt.fastsattVanligArbeidstid.nyArbeidstidPerUke shouldBe 0.0
            this.fastsatt.fastsattVanligArbeidstid.begrunnelse
                .shouldBeNull()

            this.fastsatt.sats.dagsatsMedBarnetillegg shouldBe 1077
            this.fastsatt.sats.begrunnelse shouldBe null

            this.fastsatt.samordning shouldHaveSize 1
            this.fastsatt.kvoter shouldHaveSize 2

            this.automatisk shouldBe true
        }
    }

    @Test
    fun `skal lage produkt av vedtak om avslag`() {
        rapid.sendTestMessage(avslagVedtak)

        val value = slot<ProducerRecord<String, Vedtak>>()
        verify {
            producer.send(capture(value), any())
        }

        value.isCaptured shouldBe true
        value.captured.key() shouldBe "11109233444"

        with(value.captured.value()) {
            this.ident shouldBe "11109233444"
            this.fagsakId shouldBe "123"

            this.vilkaar shouldHaveSize 8

            this.fastsatt.utfall shouldBe false
            this.fastsatt.grunnlag.shouldBeNull()
            this.fastsatt.fastsattVanligArbeidstid.shouldBeNull()
            this.fastsatt.sats.shouldBeNull()
            this.fastsatt.samordning shouldHaveSize 0
            this.fastsatt.kvoter shouldHaveSize 0

            this.automatisk shouldBe true
        }
    }

    private val innvilgelsesVedtak by lazy {
        // Generert i dp-behandling: https://github.com/navikt/dp-behandling/blob/459cbfe6e41362be45133ff2ca52d4a56ad2d1bb/mediator/src/test/kotlin/no/nav/dagpenger/behandling/PersonMediatorTest.kt#L323
        javaClass.getResource("/dp-behandling/vedtak_fattet_innvilgelse.json")!!.readText()
    }
    private val avslagVedtak by lazy {
        // Generert i dp-behandling: https://github.com/navikt/dp-behandling/blob/459cbfe6e41362be45133ff2ca52d4a56ad2d1bb/mediator/src/test/kotlin/no/nav/dagpenger/behandling/PersonMediatorTest.kt#L323
        javaClass.getResource("/dp-behandling/vedtak_fattet_avslag.json")!!.readText()
    }
}
