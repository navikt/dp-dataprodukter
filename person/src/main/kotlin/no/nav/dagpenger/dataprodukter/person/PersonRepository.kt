package no.nav.dagpenger.dataprodukter.person

interface PersonRepository {
    fun hentPerson(ident: String): Person
}
