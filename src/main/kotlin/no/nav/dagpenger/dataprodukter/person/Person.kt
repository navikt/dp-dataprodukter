package no.nav.dagpenger.dataprodukter.person

interface PersonRepository {
    fun hentPerson(ident: String): Person

    data class Person(
        val harAdressebeskyttelse: Boolean,
    )
}
