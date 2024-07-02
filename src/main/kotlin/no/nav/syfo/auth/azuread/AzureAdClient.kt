package no.nav.syfo.auth.azuread

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

@Component
class AzureAdClient(
    @Value("\${azure.app.client.id}") private val azureAppClientId: String,
    @Value("\${azure.app.client.secret}") private val azureAppClientSecret: String,
    @Value("\${azure.openid.config.token.endpoint}") private val azureTokenEndpoint: String,
    private val restTemplate: RestTemplate,
) {
    fun onBehalfOfTokenEntity(
        scope: String,
        token: String,
    ): HttpEntity<MultiValueMap<String, String>> {
        val headers = HttpHeaders()

        headers.contentType = MediaType.MULTIPART_FORM_DATA
        val body: MultiValueMap<String, String> = LinkedMultiValueMap()

        body.add(CLIENT_ID, azureAppClientId)
        body.add(SCOPE, scope)
        body.add(GRANT_TYPE, "urn:ietf:params:oauth:grant-type:jwt-bearer")
        body.add(CLIENT_SECRET, azureAppClientSecret)
        body.add(ASSERTION, token)
        body.add(CLIENT_ASSERTION_TYPE, "urn:ietf:params:oauth:grant-type:jwt-bearer")
        body.add(REQUESTED_TOKEN_USE, "on_behalf_of")

        return HttpEntity(body, headers)
    }

    fun getOnBehalfOfToken(
        scope: String,
        token: String,
    ): String =
        try {
            log.debug("Requesting new token for scope: $scope")
            val requestEntity =
                onBehalfOfTokenEntity(
                    scope = scope,
                    token = token,
                )
            val response =
                restTemplate.exchange(
                    azureTokenEndpoint,
                    HttpMethod.POST,
                    requestEntity,
                    AzureAdTokenResponse::class.java,
                )
            val tokenResponse = response.body!!

            val azureAdToken = tokenResponse.toAzureAdToken()

            azureAdToken.accessToken
        } catch (e: RestClientResponseException) {
            log.error(
                "Could not get obo-token from Azure AD for scope: " +
                    "$scope with status: ${e.statusCode} and message: ${e.responseBodyAsString}",
                e,
            )
            throw AzureAdClientException("Failed to get AzureADToken for scope: $scope", e)
        }

    companion object {
        private const val CLIENT_ID = "client_id"
        private const val SCOPE = "scope"
        private const val GRANT_TYPE = "grant_type"
        private const val CLIENT_SECRET = "client_secret"
        private const val ASSERTION = "assertion"
        private const val CLIENT_ASSERTION_TYPE = "client_assertion_type"
        private const val REQUESTED_TOKEN_USE = "requested_token_use"

        private val log = LoggerFactory.getLogger(AzureAdClient::class.java)
    }
}

class AzureAdClientException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)
