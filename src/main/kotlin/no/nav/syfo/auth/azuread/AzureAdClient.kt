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
import java.util.concurrent.ConcurrentHashMap

@Component
class AzureAdClient(
    @Value("\${azure.app.client.id}") private val azureAppClientId: String,
    @Value("\${azure.app.client.secret}") private val azureAppClientSecret: String,
    @Value("\${azure.openid.config.token.endpoint}") private val azureTokenEndpoint: String,
    private val restTemplate: RestTemplate,
) {

    fun getSystemToken(
        scopeClientId: String,
    ): String {
        val cachedToken = systemTokenCache[scopeClientId]

        return if (cachedToken?.isExpired() == false) {
            cachedToken.accessToken
        } else {
            try {
                log.debug("Requesting new token for scope: $scopeClientId")
                val requestEntity = systemTokenRequestEntity(
                    scopeClientId = scopeClientId,
                )
                val response = restTemplate.exchange(
                    azureTokenEndpoint,
                    HttpMethod.POST,
                    requestEntity,
                    AzureAdTokenResponse::class.java,
                )
                val tokenResponse = response.body!!

                val azureAdToken = tokenResponse.toAzureAdToken()

                systemTokenCache[scopeClientId] = azureAdToken
                log.info("Successfully retrieved new token for scope: $scopeClientId")
                azureAdToken.accessToken
            } catch (e: RestClientResponseException) {
                log.error(
                    "Call to get AzureADToken from AzureAD as system for scope: " +
                        "$scopeClientId with status: ${e.statusCode} and message: ${e.responseBodyAsString}",
                    e,
                )
                throw AzureAdClientException("Failed to get AzureADToken for scope: $scopeClientId", e)
            }
        }
    }

    fun systemTokenRequestEntity(
        scopeClientId: String,
    ): HttpEntity<MultiValueMap<String, String>> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        val body: MultiValueMap<String, String> = LinkedMultiValueMap()
        body.add(CLIENT_ID, azureAppClientId)
        body.add(SCOPE, "api://$scopeClientId/.default")
        body.add(GRANT_TYPE, CLIENT_CREDENTIALS)
        body.add(CLIENT_SECRET, azureAppClientSecret)

        return HttpEntity(body, headers)
    }

    companion object {
        private const val CLIENT_ID = "client_id"
        private const val SCOPE = "scope"
        private const val GRANT_TYPE = "grant_type"
        private const val CLIENT_CREDENTIALS = "client_credentials"
        private const val CLIENT_SECRET = "client_secret"

        val systemTokenCache = ConcurrentHashMap<String, AzureAdToken>()
        private val log = LoggerFactory.getLogger(AzureAdClient::class.java)
    }

    // For testing
    fun clearTokenCache() {
        systemTokenCache.clear()
    }
}

class AzureAdClientException(message: String, cause: Throwable) : RuntimeException(message, cause)
