package no.nav.dagpenger.dataprodukter.produkter

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.dataprodukt.innlop.Utland
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import no.nav.dagpenger.dataprodukter.person.PersonRepository
import no.nav.dagpenger.dataprodukter.produkter.innlop.UtlandRiver
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UtlandRiverTest {
    private val dataTopic: DataTopic<Utland> = mockk(relaxed = true)
    private val personRepository = mockk<PersonRepository>()

    private val rapid by lazy {
        TestRapid().apply {
            UtlandRiver(
                rapidsConnection = this,
                dataTopic = dataTopic,
                personRepository = personRepository,
            )
        }
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `skal lage produkt utlandstilsnitt`() {
        rapid.sendTestMessage(behovJSON)

        val packet = slot<Utland>()
        verify {
            dataTopic.publiser(capture(packet))
        }

        assertTrue(packet.isCaptured)
        assertTrue(packet.captured.erUtland)
        assertEquals("DNK,FRA,NOR", packet.captured.arbeidsforholdLand)
    }
}

@Language("JSON")
private val behovJSON =
    """
    {
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
  "@id": "53a20cc5-666b-4566-ba7c-81069bfd0bec",
  "ferdig": true,
  "seksjoner": [
    {
      "fakta": [
        {
          "id": "6001",
          "svar": "SWE",
          "type": "land",
          "roller": [
            "søker"
          ],
          "grupper": [
            {
              "land": [
                "AUT",
                "HUN",
                "DEU",
                "CZE",
                "SWE",
                "CHE",
                "ESP",
                "SVN",
                "SVK",
                "ROU",
                "PRT",
                "POL",
                "NLD",
                "MLT",
                "LUX",
                "LTU",
                "LIE",
                "LVA",
                "CYP",
                "HRV",
                "ITA",
                "ISL",
                "IRL",
                "GRC",
                "FRA",
                "FIN",
                "EST",
                "DNK",
                "BGR",
                "BEL"
              ],
              "gruppeId": "faktum.hvilket-land-bor-du-i.gruppe.eøs"
            },
            {
              "land": [
                "SJM",
                "NOR"
              ],
              "gruppeId": "faktum.hvilket-land-bor-du-i.gruppe.norge-jan-mayen"
            },
            {
              "land": [
                "IMN",
                "JEY",
                "GBR"
              ],
              "gruppeId": "faktum.hvilket-land-bor-du-i.gruppe.storbritannia"
            }
          ],
          "readOnly": false,
          "gyldigeLand": [
            "AFG",
            "ALB",
            "DZA",
            "ASM",
            "AND",
            "AGO",
            "AIA",
            "ATA",
            "ATG",
            "ARG",
            "ARM",
            "ABW",
            "AZE",
            "AUS",
            "BHS",
            "BHR",
            "BGD",
            "BRB",
            "BEL",
            "BLZ",
            "BEN",
            "BMU",
            "BTN",
            "BOL",
            "BES",
            "BIH",
            "BWA",
            "BVT",
            "BRA",
            "BRN",
            "BGR",
            "BFA",
            "BDI",
            "CAN",
            "CYM",
            "CHL",
            "CXR",
            "COL",
            "COK",
            "CRI",
            "CUB",
            "CUW",
            "DNK",
            "ARE",
            "ATF",
            "PSE",
            "DOM",
            "CAF",
            "IOT",
            "DJI",
            "DMA",
            "ECU",
            "EGY",
            "GNQ",
            "CIV",
            "SLV",
            "ERI",
            "EST",
            "ETH",
            "FLK",
            "FJI",
            "PHL",
            "FIN",
            "FRA",
            "GUF",
            "PYF",
            "FRO",
            "GAB",
            "GMB",
            "GEO",
            "GHA",
            "GIB",
            "GRD",
            "GRL",
            "GLP",
            "GUM",
            "GTM",
            "GGY",
            "GIN",
            "GNB",
            "GUY",
            "HTI",
            "HMD",
            "GRC",
            "HND",
            "HKG",
            "BLR",
            "IND",
            "IDN",
            "IRQ",
            "IRN",
            "IRL",
            "ISL",
            "ISR",
            "ITA",
            "JAM",
            "JPN",
            "YEM",
            "JEY",
            "VIR",
            "VGB",
            "JOR",
            "KHM",
            "CMR",
            "CPV",
            "KAZ",
            "KEN",
            "CHN",
            "KGZ",
            "KIR",
            "CCK",
            "COM",
            "COD",
            "COG",
            "HRV",
            "KWT",
            "CYP",
            "LAO",
            "LVA",
            "LSO",
            "LBN",
            "LBR",
            "LBY",
            "LIE",
            "LTU",
            "LUX",
            "MAC",
            "MDG",
            "MKD",
            "MWI",
            "MYS",
            "MDV",
            "MLI",
            "MLT",
            "IMN",
            "MAR",
            "MHL",
            "MTQ",
            "MRT",
            "MUS",
            "MYT",
            "MEX",
            "FSM",
            "MDA",
            "MCO",
            "MNG",
            "MNE",
            "MSR",
            "MOZ",
            "MMR",
            "NAM",
            "NRU",
            "NPL",
            "NLD",
            "NZL",
            "NIC",
            "NER",
            "NGA",
            "NIU",
            "PRK",
            "MNP",
            "NFK",
            "NOR",
            "NCL",
            "OMN",
            "PAK",
            "PLW",
            "PAN",
            "PNG",
            "PRY",
            "PER",
            "PCN",
            "POL",
            "PRT",
            "PRI",
            "QAT",
            "REU",
            "ROU",
            "RUS",
            "RWA",
            "BLM",
            "SHN",
            "KNA",
            "LCA",
            "MAF",
            "SPM",
            "VCT",
            "SLB",
            "WSM",
            "SMR",
            "STP",
            "SAU",
            "SEN",
            "SRB",
            "SYC",
            "SLE",
            "SGP",
            "SXM",
            "SVK",
            "SVN",
            "SOM",
            "ESP",
            "LKA",
            "GBR",
            "SDN",
            "SUR",
            "SJM",
            "CHE",
            "SWE",
            "SWZ",
            "SYR",
            "ZAF",
            "SGS",
            "KOR",
            "SSD",
            "TJK",
            "TWN",
            "TZA",
            "THA",
            "TGO",
            "TKL",
            "TON",
            "TTO",
            "TCD",
            "CZE",
            "TUN",
            "TKM",
            "TCA",
            "TUV",
            "TUR",
            "DEU",
            "UGA",
            "UKR",
            "HUN",
            "URY",
            "USA",
            "UMI",
            "UZB",
            "VUT",
            "VAT",
            "VEN",
            "ESH",
            "VNM",
            "WLF",
            "ZMB",
            "ZWE",
            "AUT",
            "TLS",
            "ALA"
          ],
          "beskrivendeId": "faktum.hvilket-land-bor-du-i",
          "sannsynliggjoresAv": []
        },
        {
          "id": "6002",
          "svar": false,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.reist-tilbake-etter-arbeidsledig.svar.ja",
            "faktum.reist-tilbake-etter-arbeidsledig.svar.nei"
          ],
          "beskrivendeId": "faktum.reist-tilbake-etter-arbeidsledig",
          "sannsynliggjoresAv": []
        },
        {
          "id": "6005",
          "svar": false,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.reist-tilbake-en-gang-eller-mer.svar.ja",
            "faktum.reist-tilbake-en-gang-eller-mer.svar.nei"
          ],
          "beskrivendeId": "faktum.reist-tilbake-en-gang-eller-mer",
          "sannsynliggjoresAv": []
        },
        {
          "id": "6006",
          "svar": false,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.reist-i-takt-med-rotasjon.svar.ja",
            "faktum.reist-i-takt-med-rotasjon.svar.nei"
          ],
          "beskrivendeId": "faktum.reist-i-takt-med-rotasjon",
          "sannsynliggjoresAv": []
        }
      ],
      "ferdig": true,
      "beskrivendeId": "bostedsland"
    },
    {
      "fakta": [
        {
          "id": "10001",
          "svar": "faktum.mottatt-dagpenger-siste-12-mnd.svar.nei",
          "type": "envalg",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.mottatt-dagpenger-siste-12-mnd.svar.ja",
            "faktum.mottatt-dagpenger-siste-12-mnd.svar.nei",
            "faktum.mottatt-dagpenger-siste-12-mnd.svar.vet-ikke"
          ],
          "beskrivendeId": "faktum.mottatt-dagpenger-siste-12-mnd",
          "sannsynliggjoresAv": []
        }
      ],
      "ferdig": true,
      "beskrivendeId": "gjenopptak"
    },
    {
      "fakta": [
        {
          "id": "1008",
          "svar": [],
          "type": "generator",
          "roller": [
            "nav"
          ],
          "readOnly": true,
          "templates": [
            {
              "id": "1009",
              "type": "tekst",
              "roller": [
                "nav"
              ],
              "beskrivendeId": "faktum.barn-fornavn-mellomnavn"
            },
            {
              "id": "1010",
              "type": "tekst",
              "roller": [
                "nav"
              ],
              "beskrivendeId": "faktum.barn-etternavn"
            },
            {
              "id": "1011",
              "type": "localdate",
              "roller": [
                "nav"
              ],
              "beskrivendeId": "faktum.barn-foedselsdato"
            },
            {
              "id": "1012",
              "type": "land",
              "roller": [
                "nav"
              ],
              "beskrivendeId": "faktum.barn-statsborgerskap"
            },
            {
              "id": "1013",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.forsoerger-du-barnet"
            }
          ],
          "beskrivendeId": "faktum.register.barn-liste"
        },
        {
          "id": "1007",
          "svar": false,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.legge-til-egne-barn.svar.ja",
            "faktum.legge-til-egne-barn.svar.nei"
          ],
          "beskrivendeId": "faktum.legge-til-egne-barn",
          "sannsynliggjoresAv": []
        }
      ],
      "ferdig": true,
      "beskrivendeId": "barnetillegg"
    },
    {
      "fakta": [
        {
          "id": "8001",
          "svar": "2022-10-03",
          "type": "localdate",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "beskrivendeId": "faktum.dagpenger-soknadsdato",
          "sannsynliggjoresAv": []
        },
        {
          "id": "8002",
          "svar": "faktum.type-arbeidstid.svar.fast",
          "type": "envalg",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.type-arbeidstid.svar.fast",
            "faktum.type-arbeidstid.svar.varierende",
            "faktum.type-arbeidstid.svar.kombinasjon",
            "faktum.type-arbeidstid.svar.ingen-passer"
          ],
          "beskrivendeId": "faktum.type-arbeidstid",
          "sannsynliggjoresAv": []
        },
        {
          "id": "8003",
          "svar": [
            [
              {
                "id": "8004.1",
                "svar": "ASsdf",
                "type": "tekst",
                "roller": [
                  "søker"
                ],
                "readOnly": false,
                "beskrivendeId": "faktum.arbeidsforhold.navn-bedrift",
                "sannsynliggjoresAv": []
              },
              {
                "id": "8005.1",
                "svar": "NOR",
                "type": "land",
                "roller": [
                  "søker"
                ],
                "grupper": [],
                "readOnly": false,
                "gyldigeLand": [
                  "AFG",
                  "ALB",
                  "DZA",
                  "ASM",
                  "AND",
                  "AGO",
                  "AIA",
                  "ATA",
                  "ATG",
                  "ARG",
                  "ARM",
                  "ABW",
                  "AZE",
                  "AUS",
                  "BHS",
                  "BHR",
                  "BGD",
                  "BRB",
                  "BEL",
                  "BLZ",
                  "BEN",
                  "BMU",
                  "BTN",
                  "BOL",
                  "BES",
                  "BIH",
                  "BWA",
                  "BVT",
                  "BRA",
                  "BRN",
                  "BGR",
                  "BFA",
                  "BDI",
                  "CAN",
                  "CYM",
                  "CHL",
                  "CXR",
                  "COL",
                  "COK",
                  "CRI",
                  "CUB",
                  "CUW",
                  "DNK",
                  "ARE",
                  "ATF",
                  "PSE",
                  "DOM",
                  "CAF",
                  "IOT",
                  "DJI",
                  "DMA",
                  "ECU",
                  "EGY",
                  "GNQ",
                  "CIV",
                  "SLV",
                  "ERI",
                  "EST",
                  "ETH",
                  "FLK",
                  "FJI",
                  "PHL",
                  "FIN",
                  "FRA",
                  "GUF",
                  "PYF",
                  "FRO",
                  "GAB",
                  "GMB",
                  "GEO",
                  "GHA",
                  "GIB",
                  "GRD",
                  "GRL",
                  "GLP",
                  "GUM",
                  "GTM",
                  "GGY",
                  "GIN",
                  "GNB",
                  "GUY",
                  "HTI",
                  "HMD",
                  "GRC",
                  "HND",
                  "HKG",
                  "BLR",
                  "IND",
                  "IDN",
                  "IRQ",
                  "IRN",
                  "IRL",
                  "ISL",
                  "ISR",
                  "ITA",
                  "JAM",
                  "JPN",
                  "YEM",
                  "JEY",
                  "VIR",
                  "VGB",
                  "JOR",
                  "KHM",
                  "CMR",
                  "CPV",
                  "KAZ",
                  "KEN",
                  "CHN",
                  "KGZ",
                  "KIR",
                  "CCK",
                  "COM",
                  "COD",
                  "COG",
                  "HRV",
                  "KWT",
                  "CYP",
                  "LAO",
                  "LVA",
                  "LSO",
                  "LBN",
                  "LBR",
                  "LBY",
                  "LIE",
                  "LTU",
                  "LUX",
                  "MAC",
                  "MDG",
                  "MKD",
                  "MWI",
                  "MYS",
                  "MDV",
                  "MLI",
                  "MLT",
                  "IMN",
                  "MAR",
                  "MHL",
                  "MTQ",
                  "MRT",
                  "MUS",
                  "MYT",
                  "MEX",
                  "FSM",
                  "MDA",
                  "MCO",
                  "MNG",
                  "MNE",
                  "MSR",
                  "MOZ",
                  "MMR",
                  "NAM",
                  "NRU",
                  "NPL",
                  "NLD",
                  "NZL",
                  "NIC",
                  "NER",
                  "NGA",
                  "NIU",
                  "PRK",
                  "MNP",
                  "NFK",
                  "NOR",
                  "NCL",
                  "OMN",
                  "PAK",
                  "PLW",
                  "PAN",
                  "PNG",
                  "PRY",
                  "PER",
                  "PCN",
                  "POL",
                  "PRT",
                  "PRI",
                  "QAT",
                  "REU",
                  "ROU",
                  "RUS",
                  "RWA",
                  "BLM",
                  "SHN",
                  "KNA",
                  "LCA",
                  "MAF",
                  "SPM",
                  "VCT",
                  "SLB",
                  "WSM",
                  "SMR",
                  "STP",
                  "SAU",
                  "SEN",
                  "SRB",
                  "SYC",
                  "SLE",
                  "SGP",
                  "SXM",
                  "SVK",
                  "SVN",
                  "SOM",
                  "ESP",
                  "LKA",
                  "GBR",
                  "SDN",
                  "SUR",
                  "SJM",
                  "CHE",
                  "SWE",
                  "SWZ",
                  "SYR",
                  "ZAF",
                  "SGS",
                  "KOR",
                  "SSD",
                  "TJK",
                  "TWN",
                  "TZA",
                  "THA",
                  "TGO",
                  "TKL",
                  "TON",
                  "TTO",
                  "TCD",
                  "CZE",
                  "TUN",
                  "TKM",
                  "TCA",
                  "TUV",
                  "TUR",
                  "DEU",
                  "UGA",
                  "UKR",
                  "HUN",
                  "URY",
                  "USA",
                  "UMI",
                  "UZB",
                  "VUT",
                  "VAT",
                  "VEN",
                  "ESH",
                  "VNM",
                  "WLF",
                  "ZMB",
                  "ZWE",
                  "AUT",
                  "TLS",
                  "ALA"
                ],
                "beskrivendeId": "faktum.arbeidsforhold.land",
                "sannsynliggjoresAv": []
              },
              {
                "id": "8006.1",
                "svar": "faktum.arbeidsforhold.endret.svar.avskjediget",
                "type": "envalg",
                "roller": [
                  "søker"
                ],
                "readOnly": false,
                "gyldigeValg": [
                  "faktum.arbeidsforhold.endret.svar.ikke-endret",
                  "faktum.arbeidsforhold.endret.svar.avskjediget",
                  "faktum.arbeidsforhold.endret.svar.sagt-opp-av-arbeidsgiver",
                  "faktum.arbeidsforhold.endret.svar.arbeidsgiver-konkurs",
                  "faktum.arbeidsforhold.endret.svar.kontrakt-utgaatt",
                  "faktum.arbeidsforhold.endret.svar.sagt-opp-selv",
                  "faktum.arbeidsforhold.endret.svar.redusert-arbeidstid",
                  "faktum.arbeidsforhold.endret.svar.permittert"
                ],
                "beskrivendeId": "faktum.arbeidsforhold.endret",
                "sannsynliggjoresAv": [
                  {
                    "id": "8055.1",
                    "type": "dokument",
                    "roller": [
                      "søker"
                    ],
                    "readOnly": true,
                    "beskrivendeId": "faktum.dokument-arbeidsavtale",
                    "sannsynliggjoresAv": []
                  }
                ]
              },
              {
                "id": "8017.1",
                "svar": false,
                "type": "boolean",
                "roller": [
                  "søker"
                ],
                "readOnly": false,
                "gyldigeValg": [
                  "faktum.arbeidsforhold.vet-du-antall-timer-foer-mistet-jobb.svar.ja",
                  "faktum.arbeidsforhold.vet-du-antall-timer-foer-mistet-jobb.svar.nei"
                ],
                "beskrivendeId": "faktum.arbeidsforhold.vet-du-antall-timer-foer-mistet-jobb",
                "sannsynliggjoresAv": []
              },
              {
                "id": "8039.1",
                "svar": "asdf",
                "type": "tekst",
                "roller": [
                  "søker"
                ],
                "readOnly": false,
                "beskrivendeId": "faktum.arbeidsforhold.hva-er-aarsak-til-avskjediget",
                "sannsynliggjoresAv": []
              }
            ],
            [
              {
                "id": "8004.2",
                "svar": "Utenlandsjobben",
                "type": "tekst",
                "roller": [
                  "søker"
                ],
                "readOnly": false,
                "beskrivendeId": "faktum.arbeidsforhold.navn-bedrift",
                "sannsynliggjoresAv": []
              },
              {
                "id": "8005.2",
                "svar": "DNK",
                "type": "land",
                "roller": [
                  "søker"
                ],
                "grupper": [],
                "readOnly": false,
                "gyldigeLand": [
                  "AFG",
                  "ALB",
                  "DZA",
                  "ASM",
                  "AND",
                  "AGO",
                  "AIA",
                  "ATA",
                  "ATG",
                  "ARG",
                  "ARM",
                  "ABW",
                  "AZE",
                  "AUS",
                  "BHS",
                  "BHR",
                  "BGD",
                  "BRB",
                  "BEL",
                  "BLZ",
                  "BEN",
                  "BMU",
                  "BTN",
                  "BOL",
                  "BES",
                  "BIH",
                  "BWA",
                  "BVT",
                  "BRA",
                  "BRN",
                  "BGR",
                  "BFA",
                  "BDI",
                  "CAN",
                  "CYM",
                  "CHL",
                  "CXR",
                  "COL",
                  "COK",
                  "CRI",
                  "CUB",
                  "CUW",
                  "DNK",
                  "ARE",
                  "ATF",
                  "PSE",
                  "DOM",
                  "CAF",
                  "IOT",
                  "DJI",
                  "DMA",
                  "ECU",
                  "EGY",
                  "GNQ",
                  "CIV",
                  "SLV",
                  "ERI",
                  "EST",
                  "ETH",
                  "FLK",
                  "FJI",
                  "PHL",
                  "FIN",
                  "FRA",
                  "GUF",
                  "PYF",
                  "FRO",
                  "GAB",
                  "GMB",
                  "GEO",
                  "GHA",
                  "GIB",
                  "GRD",
                  "GRL",
                  "GLP",
                  "GUM",
                  "GTM",
                  "GGY",
                  "GIN",
                  "GNB",
                  "GUY",
                  "HTI",
                  "HMD",
                  "GRC",
                  "HND",
                  "HKG",
                  "BLR",
                  "IND",
                  "IDN",
                  "IRQ",
                  "IRN",
                  "IRL",
                  "ISL",
                  "ISR",
                  "ITA",
                  "JAM",
                  "JPN",
                  "YEM",
                  "JEY",
                  "VIR",
                  "VGB",
                  "JOR",
                  "KHM",
                  "CMR",
                  "CPV",
                  "KAZ",
                  "KEN",
                  "CHN",
                  "KGZ",
                  "KIR",
                  "CCK",
                  "COM",
                  "COD",
                  "COG",
                  "HRV",
                  "KWT",
                  "CYP",
                  "LAO",
                  "LVA",
                  "LSO",
                  "LBN",
                  "LBR",
                  "LBY",
                  "LIE",
                  "LTU",
                  "LUX",
                  "MAC",
                  "MDG",
                  "MKD",
                  "MWI",
                  "MYS",
                  "MDV",
                  "MLI",
                  "MLT",
                  "IMN",
                  "MAR",
                  "MHL",
                  "MTQ",
                  "MRT",
                  "MUS",
                  "MYT",
                  "MEX",
                  "FSM",
                  "MDA",
                  "MCO",
                  "MNG",
                  "MNE",
                  "MSR",
                  "MOZ",
                  "MMR",
                  "NAM",
                  "NRU",
                  "NPL",
                  "NLD",
                  "NZL",
                  "NIC",
                  "NER",
                  "NGA",
                  "NIU",
                  "PRK",
                  "MNP",
                  "NFK",
                  "NOR",
                  "NCL",
                  "OMN",
                  "PAK",
                  "PLW",
                  "PAN",
                  "PNG",
                  "PRY",
                  "PER",
                  "PCN",
                  "POL",
                  "PRT",
                  "PRI",
                  "QAT",
                  "REU",
                  "ROU",
                  "RUS",
                  "RWA",
                  "BLM",
                  "SHN",
                  "KNA",
                  "LCA",
                  "MAF",
                  "SPM",
                  "VCT",
                  "SLB",
                  "WSM",
                  "SMR",
                  "STP",
                  "SAU",
                  "SEN",
                  "SRB",
                  "SYC",
                  "SLE",
                  "SGP",
                  "SXM",
                  "SVK",
                  "SVN",
                  "SOM",
                  "ESP",
                  "LKA",
                  "GBR",
                  "SDN",
                  "SUR",
                  "SJM",
                  "CHE",
                  "SWE",
                  "SWZ",
                  "SYR",
                  "ZAF",
                  "SGS",
                  "KOR",
                  "SSD",
                  "TJK",
                  "TWN",
                  "TZA",
                  "THA",
                  "TGO",
                  "TKL",
                  "TON",
                  "TTO",
                  "TCD",
                  "CZE",
                  "TUN",
                  "TKM",
                  "TCA",
                  "TUV",
                  "TUR",
                  "DEU",
                  "UGA",
                  "UKR",
                  "HUN",
                  "URY",
                  "USA",
                  "UMI",
                  "UZB",
                  "VUT",
                  "VAT",
                  "VEN",
                  "ESH",
                  "VNM",
                  "WLF",
                  "ZMB",
                  "ZWE",
                  "AUT",
                  "TLS",
                  "ALA"
                ],
                "beskrivendeId": "faktum.arbeidsforhold.land",
                "sannsynliggjoresAv": []
              },
              {
                "id": "8006.2",
                "svar": "faktum.arbeidsforhold.endret.svar.avskjediget",
                "type": "envalg",
                "roller": [
                  "søker"
                ],
                "readOnly": false,
                "gyldigeValg": [
                  "faktum.arbeidsforhold.endret.svar.ikke-endret",
                  "faktum.arbeidsforhold.endret.svar.avskjediget",
                  "faktum.arbeidsforhold.endret.svar.sagt-opp-av-arbeidsgiver",
                  "faktum.arbeidsforhold.endret.svar.arbeidsgiver-konkurs",
                  "faktum.arbeidsforhold.endret.svar.kontrakt-utgaatt",
                  "faktum.arbeidsforhold.endret.svar.sagt-opp-selv",
                  "faktum.arbeidsforhold.endret.svar.redusert-arbeidstid",
                  "faktum.arbeidsforhold.endret.svar.permittert"
                ],
                "beskrivendeId": "faktum.arbeidsforhold.endret",
                "sannsynliggjoresAv": [
                  {
                    "id": "8055.2",
                    "type": "dokument",
                    "roller": [
                      "søker"
                    ],
                    "readOnly": true,
                    "beskrivendeId": "faktum.dokument-arbeidsavtale",
                    "sannsynliggjoresAv": []
                  }
                ]
              },
              {
                "id": "8017.2",
                "svar": false,
                "type": "boolean",
                "roller": [
                  "søker"
                ],
                "readOnly": false,
                "gyldigeValg": [
                  "faktum.arbeidsforhold.vet-du-antall-timer-foer-mistet-jobb.svar.ja",
                  "faktum.arbeidsforhold.vet-du-antall-timer-foer-mistet-jobb.svar.nei"
                ],
                "beskrivendeId": "faktum.arbeidsforhold.vet-du-antall-timer-foer-mistet-jobb",
                "sannsynliggjoresAv": []
              },
              {
                "id": "8039.2",
                "svar": "asdf",
                "type": "tekst",
                "roller": [
                  "søker"
                ],
                "readOnly": false,
                "beskrivendeId": "faktum.arbeidsforhold.hva-er-aarsak-til-avskjediget",
                "sannsynliggjoresAv": []
              }
            ]
          ],
          "type": "generator",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "templates": [
            {
              "id": "8004",
              "type": "tekst",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.navn-bedrift"
            },
            {
              "id": "8005",
              "type": "land",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.land"
            },
            {
              "id": "8006",
              "type": "envalg",
              "roller": [
                "søker"
              ],
              "gyldigeValg": [
                "faktum.arbeidsforhold.endret.svar.ikke-endret",
                "faktum.arbeidsforhold.endret.svar.avskjediget",
                "faktum.arbeidsforhold.endret.svar.sagt-opp-av-arbeidsgiver",
                "faktum.arbeidsforhold.endret.svar.arbeidsgiver-konkurs",
                "faktum.arbeidsforhold.endret.svar.kontrakt-utgaatt",
                "faktum.arbeidsforhold.endret.svar.sagt-opp-selv",
                "faktum.arbeidsforhold.endret.svar.redusert-arbeidstid",
                "faktum.arbeidsforhold.endret.svar.permittert"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.endret"
            },
            {
              "id": "8007",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.kjent-antall-timer-jobbet"
            },
            {
              "id": "8008",
              "type": "double",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.antall-timer-jobbet"
            },
            {
              "id": "8048",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.har-tilleggsopplysninger"
            },
            {
              "id": "8009",
              "type": "tekst",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.tilleggsopplysninger"
            },
            {
              "id": "8010",
              "type": "localdate",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.startdato-arbeidsforhold"
            },
            {
              "id": "8011",
              "type": "localdate",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.arbeidstid-redusert-fra-dato"
            },
            {
              "id": "8012",
              "type": "envalg",
              "roller": [
                "søker"
              ],
              "gyldigeValg": [
                "faktum.arbeidsforhold.midlertidig-med-kontraktfestet-sluttdato.svar.ja",
                "faktum.arbeidsforhold.midlertidig-med-kontraktfestet-sluttdato.svar.nei",
                "faktum.arbeidsforhold.midlertidig-med-kontraktfestet-sluttdato.svar.vet-ikke"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.midlertidig-med-kontraktfestet-sluttdato"
            },
            {
              "id": "8013",
              "type": "localdate",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.kontraktfestet-sluttdato"
            },
            {
              "id": "8014",
              "type": "localdate",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.midlertidig-arbeidsforhold-oppstartsdato"
            },
            {
              "id": "8015",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.permittertert-fra-fiskeri-naering"
            },
            {
              "id": "8016",
              "type": "periode",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.varighet"
            },
            {
              "id": "8042",
              "type": "envalg",
              "roller": [
                "søker"
              ],
              "gyldigeValg": [
                "faktum.arbeidsforhold.midlertidig-arbeidsforhold-med-sluttdato.svar.ja",
                "faktum.arbeidsforhold.midlertidig-arbeidsforhold-med-sluttdato.svar.nei",
                "faktum.arbeidsforhold.midlertidig-arbeidsforhold-med-sluttdato.svar.vet-ikke"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.midlertidig-arbeidsforhold-med-sluttdato"
            },
            {
              "id": "8017",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.vet-du-antall-timer-foer-mistet-jobb"
            },
            {
              "id": "8018",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.vet-du-antall-timer-foer-konkurs"
            },
            {
              "id": "8019",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.vet-du-antall-timer-foer-kontrakt-utgikk"
            },
            {
              "id": "8020",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.vet-du-antall-timer-foer-du-sa-opp"
            },
            {
              "id": "8021",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.vet-du-antall-timer-foer-redusert-arbeidstid"
            },
            {
              "id": "8022",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.vet-du-antall-timer-foer-permittert"
            },
            {
              "id": "8023",
              "type": "double",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.antall-timer-dette-arbeidsforhold"
            },
            {
              "id": "8024",
              "type": "periode",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.permittert-periode"
            },
            {
              "id": "8025",
              "type": "int",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.permittert-prosent"
            },
            {
              "id": "8026",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.vet-du-lonnsplikt-periode"
            },
            {
              "id": "8027",
              "type": "periode",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.naar-var-lonnsplikt-periode"
            },
            {
              "id": "8028",
              "type": "tekst",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.aarsak-til-du-sa-opp"
            },
            {
              "id": "8029",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.tilbud-om-forlengelse-eller-annen-stilling"
            },
            {
              "id": "8030",
              "type": "envalg",
              "roller": [
                "søker"
              ],
              "gyldigeValg": [
                "faktum.arbeidsforhold.svar-paa-forlengelse-eller-annen-stilling.svar.ja",
                "faktum.arbeidsforhold.svar-paa-forlengelse-eller-annen-stilling.svar.nei",
                "faktum.arbeidsforhold.svar-paa-forlengelse-eller-annen-stilling.svar.ikke-svart"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.svar-paa-forlengelse-eller-annen-stilling"
            },
            {
              "id": "8031",
              "type": "tekst",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.aarsak-til-ikke-akseptert-tilbud"
            },
            {
              "id": "8032",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.soke-forskudd-lonnsgarantimidler"
            },
            {
              "id": "8033",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.soke-forskudd-lonnsgarantimidler-i-tillegg-til-dagpenger"
            },
            {
              "id": "8034",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.godta-trekk-fra-nav-av-forskudd-fra-lonnsgarantimidler"
            },
            {
              "id": "8035",
              "type": "envalg",
              "roller": [
                "søker"
              ],
              "gyldigeValg": [
                "faktum.arbeidsforhold.har-sokt-om-lonnsgarantimidler.svar.nei",
                "faktum.arbeidsforhold.har-sokt-om-lonnsgarantimidler.svar.nei-men-skal",
                "faktum.arbeidsforhold.har-sokt-om-lonnsgarantimidler.svar.ja"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.har-sokt-om-lonnsgarantimidler"
            },
            {
              "id": "8036",
              "type": "envalg",
              "roller": [
                "søker"
              ],
              "gyldigeValg": [
                "faktum.arbeidsforhold.dekker-lonnsgarantiordningen-lonnskravet-ditt.svar.ja",
                "faktum.arbeidsforhold.dekker-lonnsgarantiordningen-lonnskravet-ditt.svar.nei",
                "faktum.arbeidsforhold.dekker-lonnsgarantiordningen-lonnskravet-ditt.svar.vet-ikke"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.dekker-lonnsgarantiordningen-lonnskravet-ditt"
            },
            {
              "id": "8037",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.utbetalt-lonn-etter-konkurs"
            },
            {
              "id": "8038",
              "type": "localdate",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.siste-dag-utbetalt-for-konkurs"
            },
            {
              "id": "8039",
              "type": "tekst",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.hva-er-aarsak-til-avskjediget"
            },
            {
              "id": "8040",
              "type": "tekst",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.vet-du-aarsak-til-sagt-opp-av-arbeidsgiver"
            },
            {
              "id": "8041",
              "type": "tekst",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.vet-du-aarsak-til-redusert-arbeidstid"
            },
            {
              "id": "8043",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.tilbud-om-annen-stilling-eller-annet-sted-i-norge"
            },
            {
              "id": "8044",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.skift-eller-turnus"
            },
            {
              "id": "8045",
              "type": "boolean",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.rotasjon"
            },
            {
              "id": "8046",
              "type": "int",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.arbeidsdager-siste-rotasjon"
            },
            {
              "id": "8047",
              "type": "int",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.arbeidsforhold.fridager-siste-rotasjon"
            },
            {
              "id": "8055",
              "type": "dokument",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.dokument-arbeidsavtale"
            },
            {
              "id": "8058",
              "type": "dokument",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.dokument-brev-fra-bobestyrer-eller-konkursforvalter"
            },
            {
              "id": "8060",
              "type": "dokument",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.dokument-arbeidsforhold-permittert"
            },
            {
              "id": "8057",
              "type": "dokument",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.dokument-timelister"
            },
            {
              "id": "8059",
              "type": "dokument",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.dokument-arbeidsavtale"
            }
          ],
          "beskrivendeId": "faktum.arbeidsforhold"
        }
      ],
      "ferdig": true,
      "beskrivendeId": "arbeidsforhold"
    },
    {
      "fakta": [
        {
          "id": "9001",
          "svar": true,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.eos-arbeid-siste-36-mnd.svar.ja",
            "faktum.eos-arbeid-siste-36-mnd.svar.nei"
          ],
          "beskrivendeId": "faktum.eos-arbeid-siste-36-mnd",
          "sannsynliggjoresAv": []
        },
        {
          "id": "9002",
          "svar": [
            [
              {
                "id": "9003.1",
                "svar": "Den gamle utenlandsjobben",
                "type": "tekst",
                "roller": [
                  "søker"
                ],
                "readOnly": false,
                "beskrivendeId": "faktum.eos-arbeidsforhold.arbeidsgivernavn",
                "sannsynliggjoresAv": []
              },
              {
                "id": "9004.1",
                "svar": "FRA",
                "type": "land",
                "roller": [
                  "søker"
                ],
                "grupper": [
                  {
                    "land": [
                      "AUT",
                      "HUN",
                      "DEU",
                      "CZE",
                      "SWE",
                      "CHE",
                      "ESP",
                      "SVN",
                      "SVK",
                      "ROU",
                      "PRT",
                      "POL",
                      "NLD",
                      "MLT",
                      "LUX",
                      "LTU",
                      "LIE",
                      "LVA",
                      "CYP",
                      "HRV",
                      "ITA",
                      "ISL",
                      "IRL",
                      "GRC",
                      "FRA",
                      "FIN",
                      "EST",
                      "DNK",
                      "BGR",
                      "BEL"
                    ],
                    "gruppeId": "faktum.eos-arbeidsforhold.land.gruppe.eøs"
                  }
                ],
                "readOnly": false,
                "gyldigeLand": [
                  "AFG",
                  "ALB",
                  "DZA",
                  "ASM",
                  "AND",
                  "AGO",
                  "AIA",
                  "ATA",
                  "ATG",
                  "ARG",
                  "ARM",
                  "ABW",
                  "AZE",
                  "AUS",
                  "BHS",
                  "BHR",
                  "BGD",
                  "BRB",
                  "BEL",
                  "BLZ",
                  "BEN",
                  "BMU",
                  "BTN",
                  "BOL",
                  "BES",
                  "BIH",
                  "BWA",
                  "BVT",
                  "BRA",
                  "BRN",
                  "BGR",
                  "BFA",
                  "BDI",
                  "CAN",
                  "CYM",
                  "CHL",
                  "CXR",
                  "COL",
                  "COK",
                  "CRI",
                  "CUB",
                  "CUW",
                  "DNK",
                  "ARE",
                  "ATF",
                  "PSE",
                  "DOM",
                  "CAF",
                  "IOT",
                  "DJI",
                  "DMA",
                  "ECU",
                  "EGY",
                  "GNQ",
                  "CIV",
                  "SLV",
                  "ERI",
                  "EST",
                  "ETH",
                  "FLK",
                  "FJI",
                  "PHL",
                  "FIN",
                  "FRA",
                  "GUF",
                  "PYF",
                  "FRO",
                  "GAB",
                  "GMB",
                  "GEO",
                  "GHA",
                  "GIB",
                  "GRD",
                  "GRL",
                  "GLP",
                  "GUM",
                  "GTM",
                  "GGY",
                  "GIN",
                  "GNB",
                  "GUY",
                  "HTI",
                  "HMD",
                  "GRC",
                  "HND",
                  "HKG",
                  "BLR",
                  "IND",
                  "IDN",
                  "IRQ",
                  "IRN",
                  "IRL",
                  "ISL",
                  "ISR",
                  "ITA",
                  "JAM",
                  "JPN",
                  "YEM",
                  "JEY",
                  "VIR",
                  "VGB",
                  "JOR",
                  "KHM",
                  "CMR",
                  "CPV",
                  "KAZ",
                  "KEN",
                  "CHN",
                  "KGZ",
                  "KIR",
                  "CCK",
                  "COM",
                  "COD",
                  "COG",
                  "HRV",
                  "KWT",
                  "CYP",
                  "LAO",
                  "LVA",
                  "LSO",
                  "LBN",
                  "LBR",
                  "LBY",
                  "LIE",
                  "LTU",
                  "LUX",
                  "MAC",
                  "MDG",
                  "MKD",
                  "MWI",
                  "MYS",
                  "MDV",
                  "MLI",
                  "MLT",
                  "IMN",
                  "MAR",
                  "MHL",
                  "MTQ",
                  "MRT",
                  "MUS",
                  "MYT",
                  "MEX",
                  "FSM",
                  "MDA",
                  "MCO",
                  "MNG",
                  "MNE",
                  "MSR",
                  "MOZ",
                  "MMR",
                  "NAM",
                  "NRU",
                  "NPL",
                  "NLD",
                  "NZL",
                  "NIC",
                  "NER",
                  "NGA",
                  "NIU",
                  "PRK",
                  "MNP",
                  "NFK",
                  "NOR",
                  "NCL",
                  "OMN",
                  "PAK",
                  "PLW",
                  "PAN",
                  "PNG",
                  "PRY",
                  "PER",
                  "PCN",
                  "POL",
                  "PRT",
                  "PRI",
                  "QAT",
                  "REU",
                  "ROU",
                  "RUS",
                  "RWA",
                  "BLM",
                  "SHN",
                  "KNA",
                  "LCA",
                  "MAF",
                  "SPM",
                  "VCT",
                  "SLB",
                  "WSM",
                  "SMR",
                  "STP",
                  "SAU",
                  "SEN",
                  "SRB",
                  "SYC",
                  "SLE",
                  "SGP",
                  "SXM",
                  "SVK",
                  "SVN",
                  "SOM",
                  "ESP",
                  "LKA",
                  "GBR",
                  "SDN",
                  "SUR",
                  "SJM",
                  "CHE",
                  "SWE",
                  "SWZ",
                  "SYR",
                  "ZAF",
                  "SGS",
                  "KOR",
                  "SSD",
                  "TJK",
                  "TWN",
                  "TZA",
                  "THA",
                  "TGO",
                  "TKL",
                  "TON",
                  "TTO",
                  "TCD",
                  "CZE",
                  "TUN",
                  "TKM",
                  "TCA",
                  "TUV",
                  "TUR",
                  "DEU",
                  "UGA",
                  "UKR",
                  "HUN",
                  "URY",
                  "USA",
                  "UMI",
                  "UZB",
                  "VUT",
                  "VAT",
                  "VEN",
                  "ESH",
                  "VNM",
                  "WLF",
                  "ZMB",
                  "ZWE",
                  "AUT",
                  "TLS",
                  "ALA"
                ],
                "beskrivendeId": "faktum.eos-arbeidsforhold.land",
                "sannsynliggjoresAv": []
              },
              {
                "id": "9005.1",
                "svar": "123123123",
                "type": "tekst",
                "roller": [
                  "søker"
                ],
                "readOnly": false,
                "beskrivendeId": "faktum.eos-arbeidsforhold.personnummer",
                "sannsynliggjoresAv": []
              },
              {
                "id": "9006.1",
                "svar": {
                  "fom": "2022-06-27",
                  "tom": "2022-08-10"
                },
                "type": "periode",
                "roller": [
                  "søker"
                ],
                "readOnly": false,
                "beskrivendeId": "faktum.eos-arbeidsforhold.varighet",
                "sannsynliggjoresAv": []
              }
            ]
          ],
          "type": "generator",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "templates": [
            {
              "id": "9003",
              "type": "tekst",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.eos-arbeidsforhold.arbeidsgivernavn"
            },
            {
              "id": "9004",
              "type": "land",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.eos-arbeidsforhold.land"
            },
            {
              "id": "9005",
              "type": "tekst",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.eos-arbeidsforhold.personnummer"
            },
            {
              "id": "9006",
              "type": "periode",
              "roller": [
                "søker"
              ],
              "beskrivendeId": "faktum.eos-arbeidsforhold.varighet"
            }
          ],
          "beskrivendeId": "faktum.eos-arbeidsforhold"
        }
      ],
      "ferdig": true,
      "beskrivendeId": "eos-arbeidsforhold"
    },
    {
      "fakta": [
        {
          "id": "3001",
          "svar": false,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.driver-du-egen-naering.svar.ja",
            "faktum.driver-du-egen-naering.svar.nei"
          ],
          "beskrivendeId": "faktum.driver-du-egen-naering",
          "sannsynliggjoresAv": []
        },
        {
          "id": "3006",
          "svar": false,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.driver-du-eget-gaardsbruk.svar.ja",
            "faktum.driver-du-eget-gaardsbruk.svar.nei"
          ],
          "beskrivendeId": "faktum.driver-du-eget-gaardsbruk",
          "sannsynliggjoresAv": []
        }
      ],
      "ferdig": true,
      "beskrivendeId": "egen-naering"
    },
    {
      "fakta": [
        {
          "id": "7001",
          "svar": false,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd.svar.ja",
            "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd.svar.nei"
          ],
          "beskrivendeId": "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd",
          "sannsynliggjoresAv": []
        }
      ],
      "ferdig": true,
      "beskrivendeId": "verneplikt"
    },
    {
      "fakta": [
        {
          "id": "5001",
          "svar": false,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.andre-ytelser-mottatt-eller-sokt.svar.ja",
            "faktum.andre-ytelser-mottatt-eller-sokt.svar.nei"
          ],
          "beskrivendeId": "faktum.andre-ytelser-mottatt-eller-sokt",
          "sannsynliggjoresAv": []
        },
        {
          "id": "5012",
          "svar": false,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.utbetaling-eller-okonomisk-gode-tidligere-arbeidsgiver.svar.ja",
            "faktum.utbetaling-eller-okonomisk-gode-tidligere-arbeidsgiver.svar.nei"
          ],
          "beskrivendeId": "faktum.utbetaling-eller-okonomisk-gode-tidligere-arbeidsgiver",
          "sannsynliggjoresAv": []
        }
      ],
      "ferdig": true,
      "beskrivendeId": "andre-ytelser"
    },
    {
      "fakta": [
        {
          "id": "2001",
          "svar": false,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.tar-du-utdanning.svar.ja",
            "faktum.tar-du-utdanning.svar.nei"
          ],
          "beskrivendeId": "faktum.tar-du-utdanning",
          "sannsynliggjoresAv": []
        },
        {
          "id": "2002",
          "svar": false,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.avsluttet-utdanning-siste-6-mnd.svar.ja",
            "faktum.avsluttet-utdanning-siste-6-mnd.svar.nei"
          ],
          "beskrivendeId": "faktum.avsluttet-utdanning-siste-6-mnd",
          "sannsynliggjoresAv": []
        },
        {
          "id": "2003",
          "svar": false,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.planlegger-utdanning-med-dagpenger.svar.ja",
            "faktum.planlegger-utdanning-med-dagpenger.svar.nei"
          ],
          "beskrivendeId": "faktum.planlegger-utdanning-med-dagpenger",
          "sannsynliggjoresAv": []
        }
      ],
      "ferdig": true,
      "beskrivendeId": "utdanning"
    },
    {
      "fakta": [
        {
          "id": "1",
          "svar": true,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.jobbe-hel-deltid.svar.ja",
            "faktum.jobbe-hel-deltid.svar.nei"
          ],
          "beskrivendeId": "faktum.jobbe-hel-deltid",
          "sannsynliggjoresAv": []
        },
        {
          "id": "5",
          "svar": true,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.jobbe-hele-norge.svar.ja",
            "faktum.jobbe-hele-norge.svar.nei"
          ],
          "beskrivendeId": "faktum.jobbe-hele-norge",
          "sannsynliggjoresAv": []
        },
        {
          "id": "8",
          "svar": true,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.alle-typer-arbeid.svar.ja",
            "faktum.alle-typer-arbeid.svar.nei"
          ],
          "beskrivendeId": "faktum.alle-typer-arbeid",
          "sannsynliggjoresAv": []
        },
        {
          "id": "9",
          "svar": true,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.bytte-yrke-ned-i-lonn.svar.ja",
            "faktum.bytte-yrke-ned-i-lonn.svar.nei"
          ],
          "beskrivendeId": "faktum.bytte-yrke-ned-i-lonn",
          "sannsynliggjoresAv": []
        }
      ],
      "ferdig": true,
      "beskrivendeId": "reell-arbeidssoker"
    },
    {
      "fakta": [
        {
          "id": "4002",
          "svar": false,
          "type": "boolean",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.tilleggsopplysninger.har-tilleggsopplysninger.svar.ja",
            "faktum.tilleggsopplysninger.har-tilleggsopplysninger.svar.nei"
          ],
          "beskrivendeId": "faktum.tilleggsopplysninger.har-tilleggsopplysninger",
          "sannsynliggjoresAv": []
        }
      ],
      "ferdig": true,
      "beskrivendeId": "tilleggsopplysninger"
    }
  ],
  "@opprettet": "2022-10-13T14:11:22.721291092",
  "versjon_id": 236,
  "@event_name": "søker_oppgave",
  "søknad_uuid": "8b6c298a-5929-4f04-a4b9-8d40de199c5c",
  "versjon_navn": "Dagpenger",
  "@forårsaket_av": {
    "id": "130c6044-d8d5-4bd7-b60c-f4f3917d9211",
    "opprettet": "2022-10-13T14:11:22.279668532",
    "event_name": "faktum_svar"
  },
  "system_read_count": 1,
  "system_participating_services": [
    {
      "id": "53a20cc5-666b-4566-ba7c-81069bfd0bec",
      "time": "2022-10-13T14:11:22.735285770",
      "image": "ghcr.io/navikt/dp-quiz/dp-quiz-mediator:68aa5d96cdb652f7f8c3d193644cd875475aa55d",
      "service": "dp-quiz-mediator",
      "instance": "dp-quiz-mediator-85fcc9cf76-65rm7"
    },
    {
      "id": "53a20cc5-666b-4566-ba7c-81069bfd0bec",
      "time": "2022-10-13T14:11:22.740974929",
      "image": "ghcr.io/navikt/dp-soknad:48455ca8df46fc45300060533e3559c0a5bd6b55",
      "service": "dp-soknad",
      "instance": "dp-soknad-689fb89694-clwf6"
    }
  ]
},
  
  "@event_name": "innsending_ferdigstilt",
  "system_read_count": 0
}
    """.trimIndent()
