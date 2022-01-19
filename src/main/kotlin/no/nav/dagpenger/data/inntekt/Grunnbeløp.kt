package no.nav.dagpenger.data.inntekt

import java.time.LocalDate

interface Grunnbeløp {
    fun gjeldendeGrunnbeløp(fom: LocalDate): Double
}
