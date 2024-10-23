package no.nav.dagpenger.dataprodukter.søknad.data

import com.fasterxml.jackson.databind.JsonNode
import java.util.SortedSet

abstract class SøknadData(
    val data: JsonNode,
) {
    abstract val bostedsland: String
    abstract val arbeidsforholdLand: SortedSet<String>

    companion object {
        fun lagMapper(data: JsonNode) =
            when (data["seksjoner"]) {
                null -> GammelSøknadData(data)
                else -> QuizSøknadData(data["seksjoner"])
            }
    }
}
