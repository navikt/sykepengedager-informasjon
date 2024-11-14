package no.nav.syfo.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.consumer.veiledertilgang.VeilederNoAccessException
import no.nav.syfo.consumer.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.db.PMaksDato
import no.nav.syfo.db.UtbetalingerDAO
import no.nav.syfo.metric.Metric
import no.nav.syfo.utils.NAV_CALL_ID_HEADER
import no.nav.syfo.utils.NAV_PERSONIDENT_HEADER
import org.springframework.mock.web.MockHttpServletResponse
import java.time.LocalDate
import java.time.LocalDateTime

class SykepengerMaxDateAzureApiV2Test :
    DescribeSpec({

        val utbetalingerDAO = mockk<UtbetalingerDAO>(relaxed = true)
        val veilederTilgangskontrollClient = mockk<VeilederTilgangskontrollClient>(relaxed = true)
        val metric = mockk<Metric>(relaxed = true)
        val tokenValidationContextHolder = mockk<TokenValidationContextHolder>(relaxed = true)

        val fnr = "12121212121"
        val controller = SykepengerMaxDateAzureApiV2(
            veilederTilgangskontrollClient,
            utbetalingerDAO = utbetalingerDAO,
            metric,
            tokenValidationContextHolder,
        )

        beforeTest {
            clearAllMocks()
        }

        describe("API returns response") {
            it("Should return formatted max date if veileder has access") {
                val maxDate = LocalDate.now().plusDays(30)
                val utbetaltTom = LocalDate.now().plusDays(20)
                every { utbetalingerDAO.fetchMaksDatoByFnr(fnr) } returns
                    PMaksDato(
                        id = "123321",
                        fnr = fnr,
                        forelopig_beregnet_slutt = maxDate,
                        utbetalt_tom = utbetaltTom,
                        gjenstaende_sykedager = "30",
                        opprettet = LocalDateTime.now().minusDays(60),
                    )

                every { veilederTilgangskontrollClient.hasAccess(any(), any(), any()) } returns true

                val headers = mapOf(
                    NAV_PERSONIDENT_HEADER to "12121212121",
                    NAV_CALL_ID_HEADER to "call-id",
                    "authorization" to "Bearer test-token",
                )

                val response = controller.getMaxDateInfo(headers)

                TestCase.assertEquals(fnr, response?.maxDate?.fnr)
            }

            it("Should throw VeilederNoAccessException if veileder has no access") {
                every { veilederTilgangskontrollClient.hasAccess(any(), any(), any()) } returns false

                val headers = mapOf(
                    NAV_PERSONIDENT_HEADER to "12121212121",
                    NAV_CALL_ID_HEADER to "call-id",
                    "authorization" to "Bearer test-token",
                )
                val response = MockHttpServletResponse()
                shouldThrow<VeilederNoAccessException> { controller.getMaxDateInfo(headers) }
            }
        }
    })
