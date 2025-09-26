package no.nav.dagpenger.dataprodukter.person

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.expediagroup.graphql.client.types.GraphQLClientError
import io.ktor.http.HttpHeaders
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.dataprodukter.oauth2.tokenProvider
import no.nav.pdl.HentPerson
import no.nav.pdl.enums.AdressebeskyttelseGradering
import org.slf4j.MDC
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

internal typealias TokenProvider = () -> String

private fun httpClient(timeoutSec: Long): HttpClient =
    HttpClient
        .create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (timeoutSec * 1000 / 2).toInt()) // f.eks. 5s ved timeoutSec=10
        .responseTimeout(Duration.ofSeconds(timeoutSec))
        .doOnConnected { connection ->
            connection
                .addHandlerLast(ReadTimeoutHandler(timeoutSec.toInt()))
                .addHandlerLast(WriteTimeoutHandler(timeoutSec.toInt()))
        }

private fun webClientWithTimeouts(
    baseUrl: String,
    timeoutSec: Long,
): WebClient.Builder =
    WebClient
        .builder()
        .baseUrl(baseUrl)
        .clientConnector(ReactorClientHttpConnector(httpClient(timeoutSec)))

class PdlPersonRepository internal constructor(
    private val client: GraphQLWebClient,
    private val tokenProvider: TokenProvider,
) : PersonRepository {
    constructor(
        endpoint: String,
        scope: String,
    ) : this(
        GraphQLWebClient(endpoint, builder = webClientWithTimeouts(endpoint, 15.seconds.inWholeMilliseconds)),
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
