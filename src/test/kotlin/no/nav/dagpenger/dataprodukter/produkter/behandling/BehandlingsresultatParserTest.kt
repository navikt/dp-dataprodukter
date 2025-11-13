package no.nav.dagpenger.dataprodukter.produkter.behandling

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.behandling.api.models.OpprinnelseDTO
import no.nav.dagpenger.behandling.api.models.RettighetsperiodeDTO
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
        kilde: OpprinnelseDTO = OpprinnelseDTO.NY,
    ) = RettighetsperiodeDTO(fom, tom, harRett, kilde)

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
                    periode(1.juni, 2.juni, true, OpprinnelseDTO.ARVET),
                    periode(3.juni, 6.juni, false),
                )
            parser.utfall(perioder) shouldBe Stans
        }

        @Test
        fun `gir Gjenopptak når går fra ikke har rett til har rett`() {
            val perioder =
                listOf(
                    periode(1.juni, 2.juni, true, OpprinnelseDTO.ARVET),
                    periode(5.juni, 8.juni, false, OpprinnelseDTO.ARVET),
                    periode(10.juni, 16.juni, true),
                )
            parser.utfall(perioder) shouldBe Gjenopptak
        }
    }
}
