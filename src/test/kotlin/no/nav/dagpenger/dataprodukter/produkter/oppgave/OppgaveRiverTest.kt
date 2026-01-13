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
            this.oppgaveId.toString() shouldBe "874bcac5-964d-496e-80ba-23046902f0ea"
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
            this.versjon shouldBe "dp-saksbehandling-1.0.0"

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

    private val oppgaveTilStatistikk = """
        {
          "@event_name": "oppgave_til_statistikk",
          "@id": "123e4567-e89b-12d3-a456-426614174000",
          "@opprettet": "2026-01-12T08:00:00.000Z",
          "system_participating_services": [
            {
              "service": "dp-saksbehandling",
              "instance": "dp-saksbehandling-795d57c777-hqztn",
              "time": "2026-01-12T08:00:00.000Z"
            }
          ],
          "oppgave": {
            "sakId": "01956789-abcd-7123-8456-123456789abc",
            "oppgaveId": "874bcac5-964d-496e-80ba-23046902f0ea",
            "behandling": {
              "id": "01956789-abcd-7123-8456-987654321abc",
              "behandlingId": "01956789-abcd-7123-8456-987654321abc",
              "tidspunkt": "2026-01-10T10:15:30",
              "basertPåBehandlingId": null,
              "utløstAv": {
                "type": "Søknad",
                "tidspunkt": "2026-01-10T10:00:00"
              }
            },
            "personIdent": "12345678901",
            "saksbehandlerIdent": "Z123456",
            "beslutterIdent": "Z987654",
            "versjon": "dp-saksbehandling-1.0.0",
            "oppgaveTilstander": [
              {
                "tilstand": "OPPRETTET",
                "tidspunkt": "2026-01-10T10:15:30"
              },
              {
                "tilstand": "UNDER_BEHANDLING",
                "tidspunkt": "2026-01-10T11:00:00"
              },
              {
                "tilstand": "FERDIGBEHANDLET",
                "tidspunkt": "2026-01-11T14:30:00"
              },
              {
                "tilstand": "FERDIG_BEHANDLET",
                "tidspunkt": "2026-01-11T15:00:00"
              }
            ]
          }
        }
    """.trimIndent()
}
