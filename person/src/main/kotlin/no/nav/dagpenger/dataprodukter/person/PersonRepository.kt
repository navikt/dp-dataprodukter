package no.nav.dagpenger.dataprodukter.person

interface PersonRepository {
    fun hentPerson(ident: String): PersonsBeskyttelseInfo
    fun hentPersonMedKode6Og7BeskyttelseInfo(ident: String): PersonsBeskyttelseInfo
}
