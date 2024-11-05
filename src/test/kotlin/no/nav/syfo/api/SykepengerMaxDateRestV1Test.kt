package no.nav.syfo.api

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.db.PMaksDato
import no.nav.syfo.db.UtbetalingerDAO
import no.nav.syfo.metric.Metric
import no.nav.syfo.utils.formatDateForLetter
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.LocalDate
import java.time.LocalDateTime

class SykepengerMaxDateRestV1Test :
    DescribeSpec({
        val utbetalingerDAO = mockk<UtbetalingerDAO>(relaxed = true)
        val metric = mockk<Metric>(relaxed = true)
        val tokenValidator = mockk<TokenValidator>(relaxed = true)
        val tokenValidationContextHolder = mockk<TokenValidationContextHolder>(relaxed = true)
        val fnr = "12121212121"
        val controller =
            SykepengerMaxDateRestApiV1(
                utbetalingerDAO = utbetalingerDAO,
                metric,
                "123",
                tokenValidationContextHolder
            ).apply {
                this.tokenValidator = tokenValidator
            }

        beforeTest {
            clearAllMocks()
        }

        describe("API returns response") {
            it("Should return formatted for letter max date with isoformat false") {
                val maxDate = LocalDate.now().plusDays(30)
                val utbetaltTom = LocalDate.now().plusDays(20)

                every { tokenValidator.validateTokenXClaims().getFnr() } returns fnr
                every { utbetalingerDAO.fetchMaksDatoByFnr(fnr) } returns
                    PMaksDato(
                        id = "123321",
                        fnr = fnr,
                        forelopig_beregnet_slutt = maxDate,
                        utbetalt_tom = utbetaltTom,
                        gjenstaende_sykedager = "30",
                        opprettet = LocalDateTime.now().minusDays(60),
                    )

                val request = MockHttpServletRequest()
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

                val response = controller.getMaxDateInfo(isoformat = "false")

                TestCase.assertEquals(formatDateForLetter(maxDate), response?.maxDate)
                TestCase.assertEquals(formatDateForLetter(utbetaltTom), response?.utbetaltTom)
            }
        }

        it("Should return raw max date with isoformat true") {
            val maxDate = LocalDate.now().plusDays(30)
            val utbetaltTom = LocalDate.now().plusDays(20)

            every { tokenValidator.validateTokenXClaims().getFnr() } returns fnr
            every { utbetalingerDAO.fetchMaksDatoByFnr(fnr) } returns
                PMaksDato(
                    id = "123321",
                    fnr = fnr,
                    forelopig_beregnet_slutt = maxDate,
                    utbetalt_tom = utbetaltTom,
                    gjenstaende_sykedager = "30",
                    opprettet = LocalDateTime.now().minusDays(60),
                )

            val request = MockHttpServletRequest()
            RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

            val response = controller.getMaxDateInfo(isoformat = "true")

            TestCase.assertEquals(maxDate.toString(), response?.maxDate)
            TestCase.assertEquals(utbetaltTom.toString(), response?.utbetaltTom)
        }
    })
