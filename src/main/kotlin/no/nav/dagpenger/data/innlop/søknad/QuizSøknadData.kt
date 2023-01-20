package no.nav.dagpenger.data.innlop.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import mu.KotlinLogging
import java.util.SortedSet

private val logger = KotlinLogging.logger { }

internal class QuizSøknadData(data: JsonNode) : SøknadData(data) {
    override val bostedsland: String
        get() = getFaktum("faktum.hvilket-land-bor-du-i")["svar"].asText()
    override val arbeidsforholdLand: SortedSet<String>
        get() = (
            getFakta("faktum.arbeidsforhold.land").map { it["svar"].asText() } +
                getFakta("faktum.eos-arbeidsforhold.land").map { it["svar"].asText() }
            ).toSortedSet()

    private fun getFaktum(faktumId: String) = getFakta(faktumId).single()

    private fun getFakta(faktumId: String) = getFakta(faktumId, data)

    private fun getFakta(faktumId: String, seksjoner: JsonNode): List<JsonNode> {
        return alleFakta(seksjoner)
            .filter {
                it["beskrivendeId"].asText() == faktumId
            }
    }

    private fun alleFakta(seksjoner: JsonNode) = seksjoner
        .flatMap { seksjon -> seksjon["fakta"] }
        .flatMap { fakta ->
            when (fakta["type"].asText()) {
                "generator" -> {
                    val navn = fakta["beskrivendeId"].asText()
                    val svar = if (fakta.has("svar")) fakta["svar"] else emptyList<List<*>>().also {
                        logger.warn { "Generator $navn mangler svar " }
                    }
                    svar
                        .flatten()
                        .map {
                            it as ObjectNode
                            val indeks = it["id"].asText().let { id -> id.split(".")[1] }
                            it.put("gruppe", navn)
                            it.put("gruppeId", "$navn.$indeks")
                        }
                }

                "flervalg" -> {
                    fakta["svar"].map {
                        val flervalg: ObjectNode = fakta.deepCopy()
                        flervalg.put("svar", it.asText())
                    }
                }

                else -> listOf(fakta)
            }
        }

    val fakta
        get() = alleFakta(data).map {
            val gruppe = it["gruppe"]?.asText()
            val gruppeId = it["gruppeId"]?.asText()
            Faktum(it["beskrivendeId"].asText(), it["type"].asText(), it["svar"].asText(), gruppe, gruppeId)
        }.filterNot { it.erFritekst }

    data class Faktum(
        val beskrivendeId: String,
        val type: String,
        val svar: String,
        val gruppe: String?,
        val gruppeId: String?
    ) {
        val erFritekst = type == "tekst"
    }
}
