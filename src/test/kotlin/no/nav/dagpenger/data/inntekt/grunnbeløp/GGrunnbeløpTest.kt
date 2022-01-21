package no.nav.dagpenger.data.inntekt.grunnbeløp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GGrunnbeløpTest {
    private val g = GGrunnbeløp()

    @Test
    fun `gir riktig grunnbeløp`() {
        val now = LocalDate.now()
        assertEquals(106399.0, g.gjeldendeGrunnbeløp(now))
        assertEquals(101351.0, g.gjeldendeGrunnbeløp(now.minusYears(1)))
        assertEquals(99858.0, g.gjeldendeGrunnbeløp(now.minusYears(2)))
        assertEquals(106399.0, g.gjeldendeGrunnbeløp(now))
    }
}
