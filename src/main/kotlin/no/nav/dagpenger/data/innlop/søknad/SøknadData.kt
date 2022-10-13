package no.nav.dagpenger.data.innlop.søknad

import com.fasterxml.jackson.databind.JsonNode

internal abstract class SøknadData(protected val data: JsonNode) {
    abstract val bostedsland: String
    abstract val arbeidsforholdLand: List<String>

    companion object {
        fun lagMapper(data: JsonNode) = when (data["seksjoner"]) {
            null -> GammeltSøknadFormat(data)
            else -> QuizSøknadFormat(data)
        }
    }
}
