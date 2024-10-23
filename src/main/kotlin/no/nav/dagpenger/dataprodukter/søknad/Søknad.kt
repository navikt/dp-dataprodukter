package no.nav.dagpenger.dataprodukter.søknad

import java.util.UUID

internal data class Søknad(
    val søknadId: UUID,
    val søknadType: String,
    val data: QuizSøknadData,
)
