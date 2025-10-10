package no.nav.dagpenger.dataprodukter.person

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import no.nav.dagpenger.dataprodukter.PdlMock
import no.nav.pdl.enums.AdressebeskyttelseGradering
import no.nav.pdl.hentperson.Adressebeskyttelse
import no.nav.pdl.hentperson.Person
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@WireMockTest
class PdlPersonRepositoryTest {
    private companion object {
        private val ugradertPerson = Person(listOf(Adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT)))
        private val kode7 = Person(listOf(Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG)))
        private val kode6 = Person(listOf(Adressebeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG)))
        private val ident = "12312312311"
        private lateinit var pdlClient: GraphQLKtorClient

        @JvmStatic
        @BeforeAll
        fun setup(wiremock: WireMockRuntimeInfo) {
            pdlClient = graphQLKtorClient("http://localhost:${wiremock.httpPort}/graphql")
        }
    }

    private val repo = PdlPersonRepository(pdlClient) { "string" }

    @Test
    fun `Hent person med kode7`(wiremock: WireMockRuntimeInfo) {
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
        PdlMock.errorResponse()
        val exception =
            assertThrows<RuntimeException> {
                repo.hentPerson(ident).harAdressebeskyttelse
            }

        assertEquals(
            "Kall mot PDL feilet. Feil: KotlinxGraphQLError(message=foo, locations=null, path=null, extensions=null)",
            exception.message,
        )
    }

    private fun withPdl(
        person: Person,
        block: PersonRepository.() -> Unit,
    ) {
        PdlMock.successResponse(person)

        block(repo)
    }
}
