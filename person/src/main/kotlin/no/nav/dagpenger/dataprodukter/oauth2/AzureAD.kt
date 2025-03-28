package no.nav.dagpenger.dataprodukter.oauth2

import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.PropertyGroup
import com.natpryce.konfig.getValue
import com.natpryce.konfig.stringType
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.TokenResponse
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.token.AccessToken
import no.nav.dagpenger.dataprodukter.person.TokenProvider
import java.net.URI

private object azure : PropertyGroup() {
    val app_client_id by stringType
    val app_client_secret by stringType
    val openid_config_token_endpoint by stringType
}

private val config = EnvironmentVariables()

fun tokenProvider(scope: String): TokenProvider {
    val azureAD = AzureAD(scope)
    return { azureAD.token().toAuthorizationHeader() }
}

class AzureAD internal constructor(
    private val scope: Scope,
    private val tokenEndpoint: URI,
    private val clientAuth: ClientAuthentication,
) {
    constructor(vararg scope: String) : this(
        Scope(*scope),
        URI(config[azure.openid_config_token_endpoint]),
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
