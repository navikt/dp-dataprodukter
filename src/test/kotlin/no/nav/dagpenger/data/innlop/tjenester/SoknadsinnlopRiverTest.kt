package no.nav.dagpenger.data.innlop.tjenester

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.data.innlop.Ident
import no.nav.dagpenger.data.innlop.Soknadsinnlop
import no.nav.dagpenger.data.innlop.kafka.DataTopic
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.apache.kafka.clients.producer.KafkaProducer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class SoknadsinnlopRiverTest {
    private val producer = mockk<KafkaProducer<String, Soknadsinnlop>>(relaxed = true)
    private val producerIdent = mockk<KafkaProducer<String, Ident>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val identTopic = DataTopic(producerIdent, "ident")
    private val rapid by lazy {
        TestRapid().apply {
            SoknadsinnlopRiver(
                rapidsConnection = this,
                dataTopic = dataTopic,
                identTopic = identTopic
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
        rapid.sendTestMessage(behovJSON)

        verify {
            producer.send(any(), any())
            producerIdent.send(any(), any())
        }
    }
}

@Language("JSON")
private val behovJSON = """{
  "@id": "1aac8cc3-d83f-49c0-aff5-27c56bc1f97d",
  "@opprettet": "2022-06-09T12:40:12.949953",
  "journalpostId": "12455",
  "datoRegistrert": "2022-06-09T12:40:12.945728",
  "skjemaKode": "test",
  "tittel": "Tittel",
  "type": "NySøknad",
  "fødselsnummer": "12345678901",
  "aktørId": "1234455",
  "fagsakId": "1234",
  "søknadsData": {
    "test": "test"
  },
  "@event_name": "innsending_ferdigstilt",
  "system_read_count": 0
}
""".trimIndent()
