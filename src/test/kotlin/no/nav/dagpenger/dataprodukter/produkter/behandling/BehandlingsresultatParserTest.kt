package no.nav.dagpenger.dataprodukter.produkter.behandling

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.dataprodukt.behandling.Rettighetsperiode
import no.nav.dagpenger.dataprodukter.helpers.juni
import no.nav.dagpenger.dataprodukter.produkter.behandling.BehandlingsresultatParser.Utfall.Gjenopptak
import no.nav.dagpenger.dataprodukter.produkter.behandling.BehandlingsresultatParser.Utfall.Stans
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingsresultatParserTest {
    private val parser = BehandlingsresultatParser(mockk())

    private fun periode(
        fom: LocalDate,
        tom: LocalDate,
        harRett: Boolean,
        kilde: String = "Ny",
    ) = Rettighetsperiode(fom, tom, harRett, kilde)

    @Nested
    inner class UtfallForEnPeriode {
        @Test
        fun `gir Innvilgelse når ny og har rett`() {
            val perioder = listOf(periode(1.juni, 2.juni, true))
            parser.utfall(perioder) shouldBe BehandlingsresultatParser.Utfall.Innvilgelse
        }

        @Test
        fun `gir Avslag når ny og ikke har rett`() {
            val perioder = listOf(periode(1.juni, 2.juni, false))
            parser.utfall(perioder) shouldBe BehandlingsresultatParser.Utfall.Avslag
        }
    }

    @Nested
    inner class UtfallForFlerePerioder {
        @Test
        fun `gir Stans når går fra har rett til ikke har rett`() {
            val perioder =
                listOf(
                    periode(1.juni, 2.juni, true, "Arvet"),
                    periode(3.juni, 6.juni, false, "Ny"),
                )
            parser.utfall(perioder) shouldBe Stans
        }

        @Test
        fun `gir Gjenopptak når går fra ikke har rett til har rett`() {
            val perioder =
                listOf(
                    periode(1.juni, 2.juni, true, "Arvet"),
                    periode(5.juni, 8.juni, false, "Arvet"),
                    periode(10.juni, 16.juni, true, "Ny"),
                )
            parser.utfall(perioder) shouldBe Gjenopptak
        }
    }
}
