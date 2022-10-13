package no.nav.dagpenger.data.innlop.søknad

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.data.innlop.erEØS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class QuizSøknadFormatTest {
    private val søknadData by lazy {
        QuizSøknadFormat(jacksonObjectMapper().readTree(object {}.javaClass.getResourceAsStream("/soknadsdata_nytt_format.json")))
    }

    @Test
    fun getBostedsland() {
        assertEquals("SWE", søknadData.bostedsland)
    }

    @Test
    fun getArbeidsforholdEos() {
        assertTrue(søknadData.arbeidsforholdLand.any { it.erEØS() })
    }

    @Test
    fun getArbeidsforholdLand() {
        assertEquals(setOf("DNK", "FRA", "NOR"), søknadData.arbeidsforholdLand)
    }
}
