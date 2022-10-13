package no.nav.dagpenger.data.innlop.søknad

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GammeltSøknadFormatTest {
    private val søknadData by lazy {
        GammeltSøknadFormat(jacksonObjectMapper().readTree(object {}.javaClass.getResourceAsStream("/soknadsdata_gammelt_format.json")))
    }

    @Test
    fun getBostedsland() {
        assertEquals("SWE", søknadData.bostedsland)
    }

    @Test
    fun getArbeidsforholdLand() {
        assertEquals(setOf("FRA", "GER", "NOR", "SWE"), søknadData.arbeidsforholdLand)
    }
}
