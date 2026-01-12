package no.nav.dagpenger.dataprodukter.produkter.oppgave

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.dataprodukt.oppgave.Oppgave
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OppgaveRiverTest {
    private val producer = mockk<KafkaProducer<String, Oppgave>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data-oppgave")
    private val rapid by lazy {
        TestRapid().apply {
            OppgaveRiver(
                rapidsConnection = this,
                dataTopic = dataTopic,
            )
        }
    }

    init {
        System.setProperty("kafka_produkt_oppgave_topic", "foobar")
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `kan videreformidle oppgave til statistikk`() {
        rapid.sendTestMessage(oppgaveTilStatistikk)

        val value = slot<ProducerRecord<String, Oppgave>>()
        verify {
            producer.send(capture(value), any())
        }

        value.isCaptured shouldBe true
        val key = value.captured.key()

        with(value.captured.value()) {
            this.personIdent shouldBe key
            this.personIdent shouldBe "12345678901"
            this.sakId.toString() shouldBe "01956789-abcd-7123-8456-123456789abc"
            this.behandling.behandlingId.toString() shouldBe "01956789-abcd-7123-8456-987654321abc"
            this.behandling.basertPaaBehandlingId shouldBe null
            this.behandling.utloestAv.type shouldBe "Søknad"
            this.saksbehandlerIdent shouldBe "Z123456"
            this.beslutterIdent shouldBe "Z987654"
            this.oppgaveTilstander.size shouldBe 4
            this.oppgaveTilstander[0].tilstand shouldBe "OPPRETTET"
            this.oppgaveTilstander[1].tilstand shouldBe "UNDER_BEHANDLING"
            this.oppgaveTilstander[2].tilstand shouldBe "FERDIGBEHANDLET"
            this.oppgaveTilstander[3].tilstand shouldBe "FERDIG_BEHANDLET"
        }
    }

    @Test
    fun `kan videreformidle oppgave uten saksbehandler og beslutter`() {
        val oppgaveUtenBehandlere = oppgaveTilStatistikk
            .replace("\"saksbehandlerIdent\": \"Z123456\"", "\"saksbehandlerIdent\": null")
            .replace("\"beslutterIdent\": \"Z987654\"", "\"beslutterIdent\": null")

        rapid.sendTestMessage(oppgaveUtenBehandlere)

        val value = slot<ProducerRecord<String, Oppgave>>()
        verify {
            producer.send(capture(value), any())
        }

        value.isCaptured shouldBe true

        with(value.captured.value()) {
            this.personIdent shouldBe "12345678901"
            this.saksbehandlerIdent shouldBe null
            this.beslutterIdent shouldBe null
        }
    }

    private val oppgaveTilStatistikk by lazy {
        javaClass.getResource("/oppgave/oppgave_til_statistikk.json")!!.readText()
    }
}
