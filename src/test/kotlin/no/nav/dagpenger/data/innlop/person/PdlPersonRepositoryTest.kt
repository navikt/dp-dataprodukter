package no.nav.dagpenger.data.innlop.person

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.expediagroup.graphql.client.types.GraphQLClientError
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.pdl.HentPerson
import no.nav.pdl.enums.AdressebeskyttelseGradering
import no.nav.pdl.hentperson.Adressebeskyttelse
import no.nav.pdl.hentperson.Person
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PdlPersonRepositoryTest {
    private val pdlMock = mockk<GraphQLWebClient>()
    private val repo = PdlPersonRepository(pdlMock) { "string" }

    private companion object {
        private val ugradertPerson = Person(listOf(Adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT)))
        private val kode7 = Person(listOf(Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG)))
        private val kode6 = Person(listOf(Adressebeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG)))
        private const val ident = "12312312311"
    }

    @Test
    fun `Hent person med kode7`() {
        withPdl(kode7) {
            assertFalse(hentPerson(ident).harAdressebeskyttelse)
        }
    }

    @Test
    fun `Hent person med kode6`() {
        withPdl(kode6) {
            assertTrue(hentPerson(ident).harAdressebeskyttelse)
        }
    }

    @Test
    fun `Hent person uten adressebeskyttelse`() {
        withPdl(ugradertPerson) {
            assertFalse(hentPerson(ident).harAdressebeskyttelse)
        }
    }

    @Test
    fun `Hent person gir feil`() {
        coEvery {
            pdlMock.execute(any<HentPerson>(), any())
        } returns errorResponse()
        val exception = assertThrows<RuntimeException> {
            repo.hentPerson(ident).harAdressebeskyttelse
        }

        assertEquals("Kall mot PDL feilet. Feil: TestGraphQLClientError(message=foo)", exception.message)
    }

    private fun withPdl(person: Person, block: PersonRepository.() -> Unit) {
        coEvery {
            pdlMock.execute(any<HentPerson>(), any())
        } returns successResponse(person)

        block(repo)
    }

    private fun successResponse(person: Person) = response(HentPerson.Result(person))
    private fun errorResponse() =
        response(HentPerson.Result(null), listOf(TestGraphQLClientError()))

    private fun <T> response(data: T? = null, errors: List<GraphQLClientError> = emptyList()) =
        TestResponse(data, errors)

    private data class TestResponse<T>(override val data: T?, override val errors: List<GraphQLClientError>?) :
        GraphQLClientResponse<T>

    private data class TestGraphQLClientError(override val message: String = "foo") : GraphQLClientError
}
