package no.nav.dagpenger.data.innlop.oauth2

import com.nimbusds.oauth2.sdk.ClientCredentialsGrant
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.TokenResponse
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.token.AccessToken
import no.nav.dagpenger.data.innlop.azure
import no.nav.dagpenger.data.innlop.config
import java.net.URI

class AzureAD internal constructor(
    private val scope: Scope,
    private val tokenEndpoint: URI,
    private val clientAuth: ClientAuthentication,
) {
    constructor(vararg scope: String) : this(
        Scope(*scope),
        URI(config[azure.app_config_token_endpoint]),
        ClientSecretBasic(ClientID(config[azure.app_client_id]), Secret(config[azure.app_client_secret])),
    )

    private val tokenRequest: TokenRequest
        get() = TokenRequest(tokenEndpoint, clientAuth, ClientCredentialsGrant(), scope)

    fun token(): AccessToken {
        val response = TokenResponse.parse(tokenRequest.toHTTPRequest().send())
        if (!response.indicatesSuccess()) {
            throw RuntimeException(response.toErrorResponse().toString())
        }

        return response.toSuccessResponse().tokens.accessToken
    }
}
