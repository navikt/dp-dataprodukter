package no.nav.dagpenger.data.innlop.person

interface PersonRepository {
    fun hentPerson(ident: String): Person
    data class Person(val harAdressebeskyttelse: Boolean)
}
