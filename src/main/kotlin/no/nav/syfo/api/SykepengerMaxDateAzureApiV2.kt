package no.nav.syfo.api

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.auth.TokenUtil.TokenIssuer.AZUREAD
import no.nav.syfo.consumer.veiledertilgang.VeilederNoAccessException
import no.nav.syfo.consumer.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.db.PMaksDato
import no.nav.syfo.db.UtbetalingerDAO
import no.nav.syfo.logger
import no.nav.syfo.metric.Metric
import no.nav.syfo.metric.TimerBuilderName
import no.nav.syfo.utils.NAV_CALL_ID_HEADER
import no.nav.syfo.utils.NAV_PERSONIDENT_HEADER
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import java.time.Duration
import java.time.Instant

@RestController
@ProtectedWithClaims(issuer = AZUREAD)
@RequestMapping("/")
class SykepengerMaxDateAzureApiV2(
    private val veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    val utbetalingerDAO: UtbetalingerDAO,
    private val metric: Metric,
    val tokenValidationContextHolder: TokenValidationContextHolder,
) {
    private val log = logger()

    @GetMapping("/api/azure/v2/sykepenger/maxdate", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    suspend fun getMaxDateInfo(
        @RequestHeader headers: Map<String, String>,
    ): SykepengerMaxDateAzureV2Response? {
        val timer =
            metric.createTimer(
                "get_sykepenger_max_date_azure_api_v2",
                TimerBuilderName.REST_CALL_LATENCY.name,
            )
        val start = Instant.now()
        try {
            val personIdent =
                headers[NAV_PERSONIDENT_HEADER]
                    ?: throw IllegalArgumentException(
                        "Failed to get maxDate: No $NAV_PERSONIDENT_HEADER supplied in request header",
                    )

            val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, AZUREAD)

            val callId = headers[NAV_CALL_ID_HEADER].toString()

            if (veilederTilgangskontrollClient.hasAccess(personIdent, token, callId)) {
                val pMaksDato = utbetalingerDAO.fetchMaksDatoByFnr(fnr = personIdent)

                log.info("Fetched sykepengerMaxDate from database: Beregnet slutt: ${pMaksDato?.forelopig_beregnet_slutt}, " +
                        " Utbetalt tom: ${pMaksDato?.utbetalt_tom}, behandlet tom: ${pMaksDato?.tom}")

                return SykepengerMaxDateAzureV2Response(maxDate = pMaksDato)
            } else {
                throw VeilederNoAccessException()
            }
        } finally {
            val end = Instant.now()
            val duration = Duration.between(start, end)
            timer.record(duration)
        }
    }

    data class SykepengerMaxDateAzureV2Response(
        val maxDate: PMaksDato?,
    ) : Serializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}
