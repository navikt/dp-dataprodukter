package no.nav.dagpenger.dataprodukter.søknad

import no.nav.dagpenger.dataprodukter.søknad.data.QuizSøknadData
import java.util.UUID

data class Søknad(
    val søknadId: UUID,
    val søknadType: String,
    val data: QuizSøknadData,
)
