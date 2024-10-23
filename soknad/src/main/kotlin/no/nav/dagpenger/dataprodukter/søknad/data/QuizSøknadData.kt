package no.nav.dagpenger.dataprodukter.søknad.data

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.dagpenger.dataprodukter.søknad.objectMapper

internal class QuizSøknadData(
    data: JsonNode,
) : SøknadData(data) {
    private val bostedsland
        get() = getFaktum("faktum.hvilket-land-bor-du-i")["svar"].asText()
    private val arbeidsforholdLand
        get() =
            getFakta("faktum.arbeidsforhold.land").map { it["svar"].asText() } +
                getFakta("faktum.eos-arbeidsforhold.land").map { it["svar"].asText() }

    override val utenlandstilsnitt: Utenlandstilsnitt
        get() = Utenlandstilsnitt(bostedsland, arbeidsforholdLand)

    private fun getFaktum(faktumId: String) = getFakta(faktumId).single()

    private fun getFakta(faktumId: String) = getFakta(faktumId, data)

    private fun getFakta(
        faktumId: String,
        seksjoner: JsonNode,
    ): List<JsonNode> =
        alleFakta(seksjoner)
            .filter {
                it["beskrivendeId"].asText() == faktumId
            }

    private fun alleFakta(seksjoner: JsonNode) =
        seksjoner
            .flatMap { seksjon -> seksjon["fakta"] }
            .flatMap { fakta ->
                when (fakta["type"].asText()) {
                    "generator" -> {
                        val navn = fakta["beskrivendeId"].asText()
                        val svar =
                            if (fakta.has("svar")) {
                                fakta["svar"]
                            } else {
                                emptyList<List<*>>()
                            }
                        svar
                            .flatten()
                            .map { generatorSvar ->
                                generatorSvar as ObjectNode
                                val indeks = generatorSvar["id"].asText().let { id -> id.split(".")[1] }
                                generatorSvar.put("gruppe", navn)
                                generatorSvar.put("gruppeId", "$navn.$indeks")

                                when (generatorSvar["type"].asText()) {
                                    "periode" -> {
                                        periode(generatorSvar)
                                    }

                                    else -> generatorSvar
                                }
                            }
                    }

                    "flervalg" -> {
                        fakta["svar"].map {
                            val flervalg: ObjectNode = fakta.deepCopy()
                            flervalg.put("svar", it.asText())
                        }
                    }

                    "periode" -> {
                        listOf(periode(fakta))
                    }

                    else -> listOf(fakta)
                }
            }

    private fun periode(node: JsonNode): ObjectNode {
        val svar = node["svar"] as ObjectNode
        val periode = node.deepCopy() as ObjectNode
        periode.put("svar", objectMapper.writeValueAsString(svar))
        return periode
    }

    override val fakta: List<Faktum>
        get() =
            alleFakta(data)
                .map {
                    val gruppe = it["gruppe"]?.asText()
                    val gruppeId = it["gruppeId"]?.asText()
                    Faktum(it["beskrivendeId"].asText(), it["type"].asText(), it["svar"].asText(), gruppe, gruppeId)
                }.filterNot { it.erFritekst }
}
