package no.nav.dagpenger.dataprodukter.søknad.data

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.dataprodukter.søknad.objectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class QuizSøknadDataTest {
    private val søknadData by lazy {
        SøknadData.lagMapper(objectMapper.readTree(object {}.javaClass.getResourceAsStream("/nytt_format.json")))
    }

    @Test
    fun getBostedsland() {
        Assertions.assertEquals("SWE", søknadData.utenlandstilsnitt.bostedsland)
    }

    @Test
    fun getArbeidsforholdEos() {
        Assertions.assertTrue(søknadData.utenlandstilsnitt.erUtland)
    }

    @Test
    fun getArbeidsforholdLand() {
        Assertions.assertEquals(setOf("DNK", "FRA", "NOR"), søknadData.utenlandstilsnitt.arbeidsland)
    }

    @Test
    fun getFlervalgSvar() {
        val data = QuizSøknadData(jacksonObjectMapper().readTree(flervalgJSON))
        Assertions.assertEquals(
            listOf(
                "faktum.kun-deltid-aarsak.svar.omsorg-baby",
                "faktum.kun-deltid-aarsak.svar.redusert-helse",
            ),
            data.fakta.map { it.svar },
        )
    }

    @Test
    fun `generator mangler svar`() {
        val data = QuizSøknadData(jacksonObjectMapper().readTree(generatorUtenSvarJSON))
        Assertions.assertEquals(0, data.fakta.size)
    }

    @Test
    fun `periode svar med fom og tom`() {
        val data = QuizSøknadData(jacksonObjectMapper().readTree(periodeSvar))
        Assertions.assertEquals(
            """{"fom":"2021-01-14","tom":"2021-03-12"}""",
            data.fakta.first().svar,
        )
    }

    @Test
    fun `periode svar med uten tom`() {
        val data = QuizSøknadData(jacksonObjectMapper().readTree(periodeSvarUtenTom))
        Assertions.assertEquals(
            """{"fom":"2021-01-14"}""",
            data.fakta.first().svar,
        )
    }

    @Test
    fun `generator med flere svartyper`() {
        val data = QuizSøknadData(jacksonObjectMapper().readTree(generatorMedPeriode))
        Assertions.assertEquals("11", data.fakta.find { it.gruppeId == "f67.1" && it.type == "int" }?.svar)
        Assertions.assertEquals(
            """{"fom":"2021-01-14","tom":"2021-03-12"}""",
            data.fakta.find { it.gruppeId == "f67.1" && it.type == "periode" }?.svar,
        )
        Assertions.assertEquals("19", data.fakta.find { it.gruppeId == "f67.2" && it.type == "int" }?.svar)
        Assertions.assertEquals(
            """{"fom":"2023-01-14","tom":"2023-03-12"}""",
            data.fakta.find { it.gruppeId == "f67.2" && it.type == "periode" }?.svar,
        )
    }
}

// language=JSON
private val flervalgJSON =
    """
[
  {
    "fakta": [
      {
        "id": "2",
        "svar": [
          "faktum.kun-deltid-aarsak.svar.omsorg-baby",
          "faktum.kun-deltid-aarsak.svar.redusert-helse"
        ],
        "type": "flervalg",
        "roller": [
          "søker"
        ],
        "readOnly": false,
        "gyldigeValg": [
          "faktum.kun-deltid-aarsak.svar.redusert-helse",
          "faktum.kun-deltid-aarsak.svar.omsorg-baby",
          "faktum.kun-deltid-aarsak.svar.eneansvar-barn",
          "faktum.kun-deltid-aarsak.svar.omsorg-barn-spesielle-behov",
          "faktum.kun-deltid-aarsak.svar.skift-turnus",
          "faktum.kun-deltid-aarsak.svar.har-fylt-60",
          "faktum.kun-deltid-aarsak.svar.annen-situasjon"
        ],
        "beskrivendeId": "faktum.kun-deltid-aarsak",
        "sannsynliggjoresAv": []
      }
    ],
    "ferdig": true,
    "beskrivendeId": "reell-arbeidssoker"
  }
]
    """.trimIndent()

// language=JSON
private val generatorUtenSvarJSON =
    """
[
  {
    "fakta": [
      {
        "id": "2",
        "type": "generator",
        "roller": [
          "søker"
        ],
        "readOnly": false,
        "beskrivendeId": "faktum.kun-deltid-aarsak",
        "sannsynliggjoresAv": []
      }
    ],
    "ferdig": true,
    "beskrivendeId": "reell-arbeidssoker"
  }
]
    """.trimIndent()

// language=JSON
private val periodeSvar =
    """
[
    {
      "fakta": [
        {
          "id": "14",
          "type": "periode",
          "beskrivendeId": "eine kleine Periode",
          "svar": {
            "fom": "2021-01-14",
            "tom": "2021-03-12"
          },
          "roller": [
            "søker"
          ]
        } 
      ],
      "ferdig": true,
      "beskrivendeId": "periode"
    }
]
    """.trimIndent()

// language=JSON
private val periodeSvarUtenTom =
    """
[
    {
      "fakta": [
        {
          "id": "14",
          "type": "periode",
          "beskrivendeId": "eine kleine Periode",
          "svar": {
            "fom": "2021-01-14"
          },
          "roller": [
            "søker"
          ]
        } 
      ],
      "ferdig": true,
      "beskrivendeId": "periode"
    }
]
    """.trimIndent()

// language=JSON
private val generatorMedPeriode =
    """
     [{
    "fakta": [
        {
          "id": "67",
          "type": "generator",
          "beskrivendeId": "f67",
          "svar": [
            [
              {
                "id": "6.1",
                "type": "int",
                "beskrivendeId": "f6",
                "svar": 11,
                "roller": [
                  "søker"
                ],
                "readOnly": false
              },
              {
                "id": "7.1",
                "type": "periode",
                "beskrivendeId": "f7",
                "svar": {
                   "fom": "2021-01-14",
                   "tom": "2021-03-12"
                },
                "roller": [
                  "søker"
                ],
                "readOnly": false
              }
            ],
            [
              {
                "id": "6.2",
                "type": "int",
                "beskrivendeId": "f6",
                "svar": 19,
                "roller": [
                  "søker"
                ],
                "readOnly": false
              },
               {
                "id": "6.3",
                "type": "flervalg",
                "beskrivendeId": "f7",
                 "svar": [
                  "svar1",
                  "svar2"
                  ],
                
                "roller": [
                  "søker"
                ],
                "readOnly": false
              },
              
              {
                "id": "7.2",
                "type": "periode",
                "beskrivendeId": "f7",
                "svar": {
                   "fom": "2023-01-14",
                   "tom": "2023-03-12"
                },
                "roller": [
                  "søker"
                ],
                "readOnly": false
              },
               {
                "id": "7.3",
                "type": "flervalg",
                "beskrivendeId": "f7",
                 "svar": [
                  "svar1",
                  "svar2"
                  ],
                
                "roller": [
                  "søker"
                ],
                "readOnly": false
              }
            ]
          ],
          "roller": [
            "søker"
          ],
          "readOnly": false
        }
      ]
    }
]
    """.trimIndent()
