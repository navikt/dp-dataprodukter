package no.nav.dagpenger.data.innlop.søknad

import com.fasterxml.jackson.databind.JsonNode
import java.util.SortedSet

internal abstract class SøknadData(protected val data: JsonNode) {
    abstract val bostedsland: String
    abstract val arbeidsforholdLand: SortedSet<String>

    companion object {
        fun lagMapper(data: JsonNode) = when (data["seksjoner"]) {
            null -> GammelSøknadData(data)
            else -> QuizSøknadData(data["seksjoner"])
        }
    }
}
