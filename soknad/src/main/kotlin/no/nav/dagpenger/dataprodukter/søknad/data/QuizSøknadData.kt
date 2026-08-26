package no.nav.dagpenger.dataprodukter.søknad.data

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import no.nav.dagpenger.dataprodukter.søknad.objectMapper

internal class QuizSøknadData(
    data: JsonNode,
) : SøknadData(data) {
    private val bostedsland
        get() = getFaktum("faktum.hvilket-land-bor-du-i")["svar"].asString()
    private val arbeidsforholdLand
        get() =
            getFakta("faktum.arbeidsforhold.land").map { it["svar"].asString() } +
                getFakta("faktum.eos-arbeidsforhold.land").map { it["svar"].asString() }

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
                it["beskrivendeId"].asString() == faktumId
            }

    private fun alleFakta(seksjoner: JsonNode) =
        seksjoner
            .flatMap { seksjon -> seksjon["fakta"] }
            .flatMap { fakta ->
                when (fakta["type"].asString()) {
                    "generator" -> {
                        val navn = fakta["beskrivendeId"].asString()
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
                                val indeks = generatorSvar["id"].asString().let { id -> id.split(".")[1] }
                                generatorSvar.put("gruppe", navn)
                                generatorSvar.put("gruppeId", "$navn.$indeks")

                                when (generatorSvar["type"].asString()) {
                                    "periode" -> {
                                        periode(generatorSvar)
                                    }

                                    else -> generatorSvar
                                }
                            }
                    }

                    "flervalg" -> {
                        fakta["svar"].toList().map {
                            val flervalg = fakta.deepCopy() as ObjectNode
                            flervalg.put("svar", it.asString())
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
                    val gruppe = it["gruppe"]?.asString()
                    val gruppeId = it["gruppeId"]?.asString()
                    Faktum(it["beskrivendeId"].asString(), it["type"].asString(), it["svar"].asTextOrEmpty(), gruppe, gruppeId)
                }.filterNot { it.erFritekst }
}

/**
 * Speiler Jackson 2 sin gamle, tolerante `asText()`-oppførsel: returner tom streng for
 * container-noder (array/objekt) i stedet for å kaste [tools.jackson.databind.exc.JsonNodeException].
 */
private fun JsonNode.asTextOrEmpty(): String = if (isValueNode) asString() else ""
