package no.nav.syfo.consumer.veiledertilgang

import no.nav.syfo.auth.azuread.AzureAdClient
import no.nav.syfo.logger
import no.nav.syfo.utils.NAV_CALL_ID_HEADER
import no.nav.syfo.utils.NAV_PERSONIDENT_HEADER
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

@Component
class VeilederTilgangskontrollClient(
    private val azureAdTokenConsumer: AzureAdClient,
    @Value("\${istilgangskontroll.url}") private val baseUrl: String,
    @Value("\${istilgangskontroll.scope}") private var targetAppScope: String,
    private val restTemplate: RestTemplate,
) {
    private val log = logger()

    fun hasAccess(
        personIdent: String,
        token: String,
        callId: String,
    ): Boolean {
        val requestURL = "$baseUrl/api/tilgang/navident/person"

        try {
            val onBehalfOfToken =
                azureAdTokenConsumer.getOnBehalfOfToken(
                    scope = targetAppScope,
                    token = token,
                )

            val headers =
                HttpHeaders().apply {
                    setBearerAuth(onBehalfOfToken)
                    contentType = MediaType.APPLICATION_JSON
                    set(NAV_PERSONIDENT_HEADER, personIdent)
                    set(NAV_CALL_ID_HEADER, callId)
                }

            val response =
                restTemplate.exchange(
                    requestURL,
                    HttpMethod.GET,
                    HttpEntity<String>(headers),
                    Tilgang::class.java,
                )
            log.info("TODO response veileder tilg: ${response.body}")
            return response.body!!.erGodkjent
        } catch (e: RestClientResponseException) {
            if (e.statusCode == HttpStatusCode.valueOf(403)) { // Forbidden
                log.warn("Denied veileder access to person: ${e.message}")
            } else {
                log.error("Encountered exception during call to tilgangskontroll: ${e.message}")
            }
            return false
        } catch (e: Exception) {
            log.error("Encountered exception during call to tilgangskontroll: ${e.message}")
            return false
        } catch (e: Error) {
            log.error("Encountered error!!: ${e.message}")
            throw e
        }
    }
}
