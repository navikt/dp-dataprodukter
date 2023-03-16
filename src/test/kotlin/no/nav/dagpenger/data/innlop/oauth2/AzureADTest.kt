package no.nav.dagpenger.data.innlop.oauth2

import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.id.ClientID
import no.nav.security.mock.oauth2.withMockOAuth2Server
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AzureADTest {
    companion object {
        private const val issuerId = "default"
        private const val apiScope = "api://test-scope"
    }

    @Test
    fun token() {
        withMockOAuth2Server {
            val client = AzureAD(
                Scope(apiScope),
                tokenEndpointUrl(issuerId).toUri(),
                ClientSecretBasic(ClientID("foo"), Secret("bar")),
            )

            with(client.token()) {
                assertTrue(toAuthorizationHeader().startsWith("Bearer"))
                assertTrue(scope.contains(apiScope))
                assertTrue(lifetime > 0)
            }
        }
    }
}
