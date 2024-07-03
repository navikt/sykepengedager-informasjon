package no.nav.syfo.api

import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.syfo.consumer.veiledertilgang.VeilederNoAccessException
import no.nav.syfo.consumer.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.db.PMaksDato
import no.nav.syfo.db.UtbetalingerDAO
import no.nav.syfo.logger
import no.nav.syfo.utils.NAV_CALL_ID_HEADER
import no.nav.syfo.utils.NAV_PERSONIDENT_HEADER
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
class SykepengerMaxDateAzureApiV2(
    private val veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    val utbetalingerDAO: UtbetalingerDAO,
) {
    private val log = logger()

    @GetMapping("/api/azure/v2/sykepenger/maxdate", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    suspend fun getMaxDateInfo(
        @RequestHeader headers: Map<String, String>,
    ): SykepengerMaxDateAzureV2Response {
        val personIdent =
            headers[NAV_PERSONIDENT_HEADER]
                ?: throw IllegalArgumentException(
                    "Failed to get maxDate: No $NAV_PERSONIDENT_HEADER supplied in request header",
                )

        val token = headers["authorization"]?.removePrefix("Bearer ")
            ?: throw IllegalArgumentException("Failed to get maxDate: No Authorization header supplied")

        val callId = headers[NAV_CALL_ID_HEADER].toString()

        if (veilederTilgangskontrollClient.hasAccess(personIdent, token, callId)) {
            val sykepengerMaxDate = utbetalingerDAO.fetchMaksDatoByFnr(personIdent)

            log.info("Fetched sykepengerMaxDate from database: ${sykepengerMaxDate?.forelopig_beregnet_slutt}")
            return SykepengerMaxDateAzureV2Response(
                maxDate =
                PMaksDato(
                    id = "123",
                    fnr = "26918198953",
                    forelopig_beregnet_slutt = LocalDate.now().plusDays(33),
                    utbetalt_tom = LocalDate.now().plusDays(32),
                    gjenstaende_sykedager = "44",
                    opprettet = LocalDateTime.now().minusDays(99),
                ),
            )
            //   TODO         return SykepengerMaxDateAzureV2Response(maxDate = sykepengerMaxDate)
        } else {
            throw VeilederNoAccessException()
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
