package no.nav.dagpenger.data.innlop.søknad

import com.fasterxml.jackson.databind.JsonNode
import java.util.SortedSet

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
                "generator" -> fakta["svar"].flatten()
                else -> listOf(fakta)
            }
        }

    val fakta
        get() = alleFakta(data).map {
            Faktum(it["beskrivendeId"].asText(), it["type"].asText(), it["svar"].asText())
        }.filterNot { it.erFritekst }

    data class Faktum(val beskrivendeId: String, val type: String, val svar: String) {
        val erFritekst = type == "tekst"
    }
}
