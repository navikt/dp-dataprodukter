package no.nav.dagpenger.dataprodukter.søknad.data

import com.fasterxml.jackson.databind.JsonNode

abstract class SøknadData(
    val data: JsonNode,
) {
    abstract val fakta: List<Faktum>
    abstract val utenlandstilsnitt: Utenlandstilsnitt

    companion object {
        fun lagMapper(data: JsonNode): SøknadData {
            val root = data["seksjoner"] ?: data
            return QuizSøknadData(root)
        }
    }
}
