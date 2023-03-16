package no.nav.dagpenger.data.innlop.person

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.expediagroup.graphql.client.types.GraphQLClientError
import kotlinx.coroutines.runBlocking
import no.nav.pdl.HentPerson
import no.nav.pdl.enums.AdressebeskyttelseGradering

class PdlPersonRepository(
    private val client: GraphQLWebClient,
    private val tokenExchange: () -> String,
) : PersonRepository {
    private companion object {
        private val beskyttet = listOf(
            AdressebeskyttelseGradering.STRENGT_FORTROLIG,
            AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND,
        )
    }

    override fun hentPerson(ident: String): PersonRepository.Person {
        val person = runBlocking {
            client.execute(HentPerson(HentPerson.Variables(ident))) {
                header("Authorization", "Bearer ${tokenExchange()}")
            }.also {
                if (it.errors?.isNotEmpty() == true) {
                    throw RuntimeException(
                        "Kall mot PDL feilet. Feil: ${
                            it.errors!!.joinToString("\n", transform = GraphQLClientError::toString)
                        }",
                    )
                }
            }
        }.data!!.hentPerson

        return PersonRepository.Person(
            harAdressebeskyttelse = person!!.adressebeskyttelse.any { it.gradering in beskyttet },
        )
    }
}
