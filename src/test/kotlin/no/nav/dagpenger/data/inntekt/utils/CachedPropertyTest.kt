package no.nav.dagpenger.data.inntekt.utils

import no.nav.dagpenger.data.inntekt.utils.CachedProperty
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.time.Duration

internal class CachedPropertyTest {
    private var teller = 1
    private val test by CachedProperty(Duration.ofMillis(100)) {
        teller++
    }

    @Test
    fun `Cacher verdi i spesifisert TTL`() {
        assertSame(1, test)
        assertSame(1, test)
        Thread.sleep(50)
        assertSame(1, test)
        Thread.sleep(50)
        assertSame(2, test)
        assertSame(2, test)
    }
}
