package no.nav.dagpenger.data.innlop.søknad

import java.util.UUID

internal interface SøknadRepository {
    fun lagre(søknadId: UUID, data: QuizSøknadData)
    fun hent(søknadId: UUID): QuizSøknadData?
    fun slett(søknadId: UUID): QuizSøknadData?
}

internal class InMemorySøknadRepository : SøknadRepository {
    private val søknader = mutableMapOf<UUID, QuizSøknadData>()

    override fun lagre(søknadId: UUID, data: QuizSøknadData) = søknader.set(søknadId, data)
    override fun hent(søknadId: UUID) = søknader[søknadId]
    override fun slett(søknadId: UUID) = søknader.remove(søknadId)
}
