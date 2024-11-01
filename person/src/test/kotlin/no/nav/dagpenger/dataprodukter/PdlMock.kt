package no.nav.dagpenger.dataprodukter

import com.expediagroup.graphql.client.types.GraphQLClientError
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import no.nav.pdl.HentPerson
import no.nav.pdl.hentperson.Person

object PdlMock {
    private val objectMapper = jacksonObjectMapper()

    fun successResponse(person: Person) {
        val json = response(HentPerson.Result(person))
        stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(
                    okJson(json.toJson()),
                ),
        )
    }

    fun errorResponse() {
        val json = response(HentPerson.Result(null), listOf(TestGraphQLClientError()))
        stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(
                    okJson(json.toJson()),
                ),
        )
    }

    private fun <T> response(
        data: T? = null,
        errors: List<GraphQLClientError> = emptyList(),
    ) = TestResponse(data, errors)

    private data class TestResponse<T>(
        override val data: T?,
        override val errors: List<GraphQLClientError>?,
    ) : GraphQLClientResponse<T> {
        fun toJson(): String = objectMapper.writeValueAsString(this)
    }

    private data class TestGraphQLClientError(
        override val message: String = "foo",
    ) : GraphQLClientError
}
