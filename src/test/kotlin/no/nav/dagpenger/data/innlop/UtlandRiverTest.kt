package no.nav.dagpenger.data.innlop

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.data.innlop.tjenester.UtlandRiver
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.apache.kafka.clients.producer.KafkaProducer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class UtlandRiverTest {
    private val producer = mockk<KafkaProducer<String, Utland>>(relaxed = true)
    private val dataTopic = DataTopic(producer, "data")
    private val rapid by lazy {
        TestRapid().apply {
            UtlandRiver(
                rapidsConnection = this,
                dataTopic = dataTopic
            )
        }
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    @Disabled
    fun `skal poste inntekt ut på Kafka`() {
        rapid.sendTestMessage(behovJSON)
        val packet = slot<Utland>()
        verify {
            dataTopic.publiser(capture(packet))
        }

        assertTrue(packet.isCaptured)
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
    "fakta": [
        {
          "faktumId": 776461,
          "soknadId": 10636,
          "parrentFaktum": null,
          "key": "bostedsland.land",
          "value": "SWE",
          "faktumEgenskaper": [],
          "properties": {},
          "type": "BRUKERREGISTRERT"
        },
      {
        "faktumId": 776566,
        "soknadId": 10636,
        "parrentFaktum": null,
        "key": "arbeidsforhold",
        "value": null,
        "faktumEgenskaer": [
          {
            "faktumId": 776566,
            "soknadId": 10636,
            "key": "datofra",
            "value": "2000-01-01",
            "systemEgenskap": 0
          },
          {
            "faktumId": 776566,
            "soknadId": 10636,
            "key": "type",
            "value": "permittert",
            "systemEgenskap": 0
          },
          {
            "faktumId": 776566,
            "soknadId": 10636,
            "key": "rotasjonskiftturnus",
            "value": "nei",
            "systemEgenskap": 0
          },
          {
            "faktumId": 776566,
            "soknadId": 10636,
            "key": "lonnspliktigperiodedatofra",
            "value": "2020-03-19",
            "systemEgenskap": 0
          },
          {
            "faktumId": 776566,
            "soknadId": 10636,
            "key": "land",
            "value": "NOR",
            "systemEgenskap": 0
          },
          {
            "faktumId": 776566,
            "soknadId": 10636,
            "key": "skalHaT8VedleggForKontraktUtgaatt",
            "value": "false",
            "systemEgenskap": 0
          },
          {
            "key": "laerling",
            "value": "true",
            "faktumId": 798841,
            "soknadId": 10636,
            "systemEgenskap": 0
          },
          {
            "faktumId": 776566,
            "soknadId": 10636,
            "key": "eosland",
            "value": "false",
            "systemEgenskap": 0
          },
          {
            "faktumId": 776566,
            "soknadId": 10636,
            "key": "arbeidsgivernavn",
            "value": "Rakettforskeren",
            "systemEgenskap": 0
          },
          {
            "faktumId": 776566,
            "soknadId": 10636,
            "key": "lonnspliktigperiodedatotil",
            "value": "2020-03-20",
            "systemEgenskap": 0
          }
        ],
        "properties": {
          "arbeidsgivernavn": "Rakettforskeren",
          "eosland": "false",
          "lonnspliktigperiodedatofra": "2020-03-19",
          "skalHaT8VedleggForKontraktUtgaatt": "false",
          "datofra": "2000-01-01",
          "lonnspliktigperiodedatotil": "2020-03-20",
          "rotasjonskiftturnus": "nei",
          "land": "NOR",
          "type": "permittert"
        },
        "type": "BRUKERREGISTRERT"
      }
    ]
  },
  "@event_name": "innsending_ferdigstilt",
  "system_read_count": 0
}
""".trimIndent()
