package no.nav.syfo.api

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.db.UtbetalingerDAO
import no.nav.syfo.logger
import no.nav.syfo.utils.formatDateForLetter
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable

@RestController
@Suppress("LongParameterList")
@RequestMapping("/")
@ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
class SykepengerMaxDateRestControllerV1(val utbetalingerDAO: UtbetalingerDAO) {
    private val log = logger()
    lateinit var tokenValidator: TokenValidator

    @GetMapping("api/v1/sykepenger/maxdate", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun getMaxDateInfo(
        @RequestParam isoformat: String?,
    ): SykepengerMaxDateResponse {
        log.info("Got request in SykepengerMaxDateRestControllerV1")
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
                    it.utbetalt_tom.toString()
                } else {
                    formatDateForLetter(it.utbetalt_tom)
                }
            }
        log.info("Fetched sykepengerMaxDate from database: ${sykepengerMaxDate?.forelopig_beregnet_slutt}")
        return SykepengerMaxDateResponse(maxDate = maxDate, utbetaltTom = utbetaltTom)
    }
}

data class SykepengerMaxDateResponse(
    val maxDate: String?,
    val utbetaltTom: String?
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
