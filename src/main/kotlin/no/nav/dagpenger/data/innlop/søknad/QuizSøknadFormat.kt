package no.nav.dagpenger.data.innlop.søknad

import com.fasterxml.jackson.databind.JsonNode

internal class QuizSøknadFormat(data: JsonNode) : SøknadData(data) {
    override val bostedsland: String
        get() = getFaktum("faktum.hvilket-land-bor-du-i")["svar"].asText()
    override val arbeidsforholdLand: List<String>
        get() = getFakta("faktum.arbeidsforhold.land").map { it["svar"].asText() } +
            getFakta("faktum.eos-arbeidsforhold.land").map { it["svar"].asText() }

    private fun getFaktum(faktumId: String) = getFakta(faktumId).single()

    private fun getFakta(faktumId: String) = getFakta(faktumId, data["seksjoner"])

    private fun getFakta(faktumId: String, seksjoner: JsonNode): List<JsonNode> {
        return seksjoner
            .flatMap { seksjon -> seksjon["fakta"] }
            .flatMap { fakta ->
                when (fakta["type"].asText()) {
                    "generator" -> fakta["svar"].flatten()
                    else -> listOf(fakta)
                }
            }
            .filter {
                it["beskrivendeId"].asText() == faktumId
            }
    }
}
