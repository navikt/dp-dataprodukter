package no.nav.dagpenger.dataprodukter.produkter.oppgave

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import no.nav.dagpenger.dataprodukt.oppgave.Oppgave
import no.nav.dagpenger.dataprodukt.oppgave.OppgaveTilstand
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.ZoneOffset
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
            this.sakId.toString() shouldBe "019c04f3-1f43-7500-b4e9-a44d3f27d187"
            this.oppgaveId.toString() shouldBe "874bcac5-964d-496e-80ba-23046902f0ea"
            this.behandling.behandlingId.toString() shouldBe "01956789-abcd-7123-8456-987654321abc"
            this.behandling.basertPaaBehandlingId shouldBe null
            this.behandling.utloestAv.type shouldBe "SØKNAD"
            this.behandling.tidspunkt.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime() shouldBe
                    LocalDateTime.of(2026, 1, 10, 10, 15, 30)
            this.saksbehandlerIdent shouldBe "Z123456"
            this.beslutterIdent shouldBe "Z987654"
            this.sisteTilstandsendring.tilstand shouldBe "FERDIG_BEHANDLET"
            this.sisteTilstandsendring.tidspunkt.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime() shouldBe
                    LocalDateTime.of(2026, 1, 11, 10, 15, 30)
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

    //language=JSON
    private val oppgaveTilStatistikk =
        """{
  "@event_name": "oppgave_til_statistikk_v2",
  "oppgave": {
    "sakId": "019c04f3-1f43-7500-b4e9-a44d3f27d187",
    "oppgaveId": "874bcac5-964d-496e-80ba-23046902f0ea",
    "behandling": {
      "behandlingId": "01956789-abcd-7123-8456-987654321abc",
      "tidspunkt": "2026-01-10T10:15:30",
      "basertPåBehandlingId": null,
      "utløstAv": {
        "type": "SØKNAD",
        "tidspunkt": "2026-01-10T10:15:30"
      }
    },
    "personIdent": "12345678901",
    "saksbehandlerIdent": "Z123456",
    "beslutterIdent": "Z987654",
    "sisteTilstandsendring": {
      "tilstand": "FERDIG_BEHANDLET",
      "tidspunkt": "2026-01-11T10:15:30"
    },
    "versjon": "dp-saksbehandling-1.0.0"
  },
  "@id": "7b1d3901-8784-4ab1-8f5c-f90ab80d7918",
  "@opprettet": "2026-01-28T15:12:58.69031",
  "system_read_count": 0,
  "system_participating_services": [
    {
      "id": "7b1d3901-8784-4ab1-8f5c-f90ab80d7918",
      "time": "2026-01-28T15:12:58.690310"
    }
  ]
}""".trimIndent()
}
