package no.nav.syfo.api

import jakarta.annotation.PostConstruct
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.db.UtbetalingerDAO
import no.nav.syfo.logger
import no.nav.syfo.metric.Metric
import no.nav.syfo.metric.TimerBuilderName
import no.nav.syfo.utils.formatDateForLetter
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import java.time.Duration
import java.time.Instant

@RestController
@Suppress("LongParameterList")
@RequestMapping("/")
@ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
class SykepengerMaxDateRestApiV1(
    val utbetalingerDAO: UtbetalingerDAO,
    private val metric: Metric,
    @Value("\${ditt.sykefravaer.client.id}")
    val dittSykefravaerClientId: String,
    @Value("\${meroppfolging.frontend.client.id}")
    val meroppfolgingFrontendClientId: String,
    val tokenValidationContextHolder: TokenValidationContextHolder,
) {
    private val log = logger()
    lateinit var tokenValidator: TokenValidator

    @PostConstruct
    fun init() {
        tokenValidator = TokenValidator(
            tokenValidationContextHolder,
            listOf(dittSykefravaerClientId, meroppfolgingFrontendClientId)
        )
    }

    @GetMapping("api/v1/sykepenger/maxdate", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun getMaxDateInfo(
        @RequestParam isoformat: String?,
    ): SykepengerMaxDateResponse? {
        log.info("Got api request /v1/sykepenger/maxdate")

        val timer = metric.createTimer(
            "sykepenger_max_date_rest_api_v1",
            TimerBuilderName.REST_CALL_LATENCY.name
        )

        val start = Instant.now()

        try {
            val personIdent = tokenValidator.validateTokenXClaims().getFnr()
            val isoFormat = isoformat?.toBoolean() ?: false

            val sykepengerMaxDate = utbetalingerDAO.fetchMaksDatoByFnr(personIdent)
            val maxDate =
                sykepengerMaxDate?.let {
                    if (isoFormat) {
                        it.forelopig_beregnet_slutt.toString()
                    } else {
                        formatDateForLetter(it.forelopig_beregnet_slutt)
                    }
                }
            val utbetaltTom =
                sykepengerMaxDate?.let {
                    if (isoFormat) {
                        it.tom.toString()
                    } else {
                        formatDateForLetter(it.tom)
                    }
                }
            log.info("Fetched sykepengerMaxDate from database: ${sykepengerMaxDate?.forelopig_beregnet_slutt}")
            return SykepengerMaxDateResponse(
                maxDate = maxDate,
                utbetaltTom = utbetaltTom,
                gjenstaendeSykedager = sykepengerMaxDate?.gjenstaende_sykedager
            )
        } finally {
            val end = Instant.now()
            val duration = Duration.between(start, end)
            timer.record(duration)
        }
    }
}

data class SykepengerMaxDateResponse(
    val maxDate: String?,
    val utbetaltTom: String?,
    val gjenstaendeSykedager: String?
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
