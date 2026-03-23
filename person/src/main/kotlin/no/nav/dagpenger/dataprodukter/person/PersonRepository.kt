package no.nav.dagpenger.dataprodukter.person

interface PersonRepository {
    fun hentPerson(ident: String): Person
    fun hentPersonMedKode6OgKode7Beskyttelse(ident: String): Person
}
