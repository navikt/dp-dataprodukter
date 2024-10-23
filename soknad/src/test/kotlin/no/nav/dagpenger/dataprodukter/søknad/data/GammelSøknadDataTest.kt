package no.nav.dagpenger.dataprodukter.søknad.data

import no.nav.dagpenger.dataprodukter.søknad.objectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class GammelSøknadDataTest {
    private val søknadData by lazy {
        GammelSøknadData(objectMapper.readTree(object {}.javaClass.getResourceAsStream("/gammelt_format.json")))
    }

    @Test
    fun getBostedsland() {
        Assertions.assertEquals("SWE", søknadData.bostedsland)
    }

    @Test
    fun getArbeidsforholdLand() {
        Assertions.assertEquals(setOf("FRA", "GER", "NOR", "SWE"), søknadData.arbeidsforholdLand)
    }
}
