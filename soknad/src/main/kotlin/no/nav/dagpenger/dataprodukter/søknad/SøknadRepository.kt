package no.nav.dagpenger.dataprodukter.søknad

import java.util.UUID

interface SøknadRepository {
    fun lagre(søknad: Søknad)

    fun hent(søknadId: UUID): Søknad?

    fun slett(søknadId: UUID): Søknad?
}

class InMemorySøknadRepository : SøknadRepository {
    private val søknader = mutableMapOf<UUID, Søknad>()

    override fun lagre(søknad: Søknad) = søknader.set(søknad.søknadId, søknad)

    override fun hent(søknadId: UUID) = søknader[søknadId]

    override fun slett(søknadId: UUID) = søknader.remove(søknadId)
}
