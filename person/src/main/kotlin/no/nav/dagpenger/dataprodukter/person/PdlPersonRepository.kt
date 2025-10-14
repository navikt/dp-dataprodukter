package no.nav.dagpenger.dataprodukter.person

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.types.GraphQLClientError
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.dataprodukter.oauth2.tokenProvider
import no.nav.pdl.HentPerson
import no.nav.pdl.enums.AdressebeskyttelseGradering
import org.slf4j.MDC
import java.net.URI
import kotlin.time.Duration.Companion.seconds

internal typealias TokenProvider = () -> String

class PdlPersonRepository internal constructor(
    private val client: GraphQLKtorClient,
    private val tokenProvider: TokenProvider,
) : PersonRepository {
    constructor(
        endpoint: String,
        scope: String,
    ) : this(
        graphQLKtorClient(endpoint),
        tokenProvider(scope),
    )

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
                            error(
                                "Kall mot PDL feilet. Feil: ${
                                    it.errors!!.joinToString("\n", transform = GraphQLClientError::toString)
                                }",
                            )
                        }
                    }
            }.data?.hentPerson ?: error("PDL svarte uten data.hentPerson")

        return Person(
            harAdressebeskyttelse = person.adressebeskyttelse.any { it.gradering in beskyttet },
        )
    }
}

internal fun graphQLKtorClient(endpoint: String): GraphQLKtorClient =
    GraphQLKtorClient(
        URI.create(endpoint).toURL(),
        httpClient =
            HttpClient {
                expectSuccess = true
                install(HttpTimeout) {
                    requestTimeoutMillis = 15.seconds.inWholeMilliseconds
                    connectTimeoutMillis = 15.seconds.inWholeMilliseconds
                    socketTimeoutMillis = 15.seconds.inWholeMilliseconds
                }
                install(ContentNegotiation) {
                    json()
                }
                defaultRequest {
                    // TODO: Skrur på keep-alive for å se om vi får samme problemer som med Spring klienten
                    header(HttpHeaders.Connection, "keep-alive")
                }
            },
    )
