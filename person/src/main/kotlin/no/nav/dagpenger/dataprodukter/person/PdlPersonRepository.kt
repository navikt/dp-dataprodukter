package no.nav.dagpenger.dataprodukter.person

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.expediagroup.graphql.client.types.GraphQLClientError
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.dataprodukter.oauth2.tokenProvider
import no.nav.pdl.HentPerson
import no.nav.pdl.enums.AdressebeskyttelseGradering
import org.slf4j.MDC

internal typealias TokenProvider = () -> String

class PdlPersonRepository internal constructor(
    private val client: GraphQLWebClient,
    private val tokenProvider: TokenProvider,
) : PersonRepository {
    constructor(
        endpoint: String,
        scope: String,
    ) : this(GraphQLWebClient(endpoint), tokenProvider(scope))

    private companion object {
        private val beskyttet =
            listOf(
                AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND,
            )
    }

    override fun hentPerson(ident: String): Person {
        val person =
            runBlocking {
                client
                    .execute(HentPerson(HentPerson.Variables(ident))) {
                        header(HttpHeaders.Authorization, "Bearer ${tokenProvider()}")
                        header(HttpHeaders.XRequestId, MDC.get("journalpostId"))
                        header("behandlingsnummer", "B342")
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

        return Person(
            harAdressebeskyttelse = person!!.adressebeskyttelse.any { it.gradering in beskyttet },
        )
    }
}
