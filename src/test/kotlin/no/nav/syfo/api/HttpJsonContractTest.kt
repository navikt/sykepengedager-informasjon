package no.nav.syfo.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.auth.getFnr
import no.nav.syfo.config.kafka.jacksonMapper
import no.nav.syfo.consumer.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.db.PMaksDato
import no.nav.syfo.db.UtbetalingerDAO
import no.nav.syfo.exception.AbstractApiError
import no.nav.syfo.exception.GlobalExceptionHandler
import no.nav.syfo.exception.LogLevel
import no.nav.syfo.metric.Metric
import no.nav.syfo.utils.NAV_CALL_ID_HEADER
import no.nav.syfo.utils.NAV_PERSONIDENT_HEADER
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime

class HttpJsonContractTest {
    @Test
    fun `REST v1 serializes ISO and letter-formatted max date responses`() {
        val personIdent = "12121212121"
        val utbetalingerDAO = mockk<UtbetalingerDAO>()
        val tokenValidator = mockk<TokenValidator>()

        every { tokenValidator.validateTokenXClaims().getFnr() } returns personIdent
        every { utbetalingerDAO.fetchMaksDatoByFnr(personIdent) } returns maksDato(personIdent)

        val controller =
            SykepengerMaxDateRestApiV1(
                utbetalingerDAO = utbetalingerDAO,
                metric = mockk<Metric>(relaxed = true),
                dittSykefravaerClientId = "test-client",
                meroppfolgingFrontendClientId = "test-client",
                tokenValidationContextHolder = mockk<TokenValidationContextHolder>(),
            ).apply {
                this.tokenValidator = tokenValidator
            }
        val mockMvc = standaloneSetup(controller).build()

        mockMvc
            .perform(get("/api/v1/sykepenger/maxdate").param("isoformat", "true"))
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    """
                    {
                      "maxDate": "2026-09-30",
                      "utbetaltTom": "2026-09-20",
                      "gjenstaendeSykedager": "30"
                    }
                    """.trimIndent(),
                    JsonCompareMode.STRICT,
                ),
            )

        mockMvc
            .perform(get("/api/v1/sykepenger/maxdate").param("isoformat", "false"))
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    """
                    {
                      "maxDate": "30. September 2026",
                      "utbetaltTom": "20. September 2026",
                      "gjenstaendeSykedager": "30"
                    }
                    """.trimIndent(),
                    JsonCompareMode.STRICT,
                ),
            )
    }

    @Test
    fun `REST v1 serializes null max date response with all fields`() {
        val personIdent = "12121212121"
        val utbetalingerDAO = mockk<UtbetalingerDAO>()
        val tokenValidator = mockk<TokenValidator>()

        every { tokenValidator.validateTokenXClaims().getFnr() } returns personIdent
        every { utbetalingerDAO.fetchMaksDatoByFnr(personIdent) } returns null

        val controller =
            SykepengerMaxDateRestApiV1(
                utbetalingerDAO = utbetalingerDAO,
                metric = mockk<Metric>(relaxed = true),
                dittSykefravaerClientId = "test-client",
                meroppfolgingFrontendClientId = "test-client",
                tokenValidationContextHolder = mockk<TokenValidationContextHolder>(),
            ).apply {
                this.tokenValidator = tokenValidator
            }
        val mockMvc = standaloneSetup(controller).build()

        mockMvc
            .perform(get("/api/v1/sykepenger/maxdate"))
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    """
                    {
                      "maxDate": null,
                      "utbetaltTom": null,
                      "gjenstaendeSykedager": null
                    }
                    """.trimIndent(),
                    JsonCompareMode.STRICT,
                ),
            )
    }

    @Test
    fun `Azure v1 serializes LocalDate max date response`() {
        val personIdent = "12121212121"
        val accessClient = mockk<VeilederTilgangskontrollClient>()
        val utbetalingerDAO = mockk<UtbetalingerDAO>()

        every { accessClient.hasAccess(personIdent, "test-token", "call-id") } returns true
        every { utbetalingerDAO.fetchMaksDatoByFnr(personIdent) } returns maksDato(personIdent)

        val mockMvc =
            standaloneSetup(
                SykepengerMaxDateAzureApiV1(
                    veilederTilgangskontrollClient = accessClient,
                    utbetalingerDAO = utbetalingerDAO,
                    metric = mockk(relaxed = true),
                ),
            ).build()

        val result =
            mockMvc
                .perform(azureRequest("/api/azure/v1/sykepenger/maxdate"))
                .andExpect(request().asyncStarted())
                .andReturn()

        mockMvc
            .perform(asyncDispatch(result))
            .andExpect(status().isOk)
            .andExpect(content().json("""{"maxDate":"2026-09-30"}""", JsonCompareMode.STRICT))
    }

    @Test
    fun `Azure v1 serializes null max date response`() {
        val personIdent = "12121212121"
        val accessClient = mockk<VeilederTilgangskontrollClient>()
        val utbetalingerDAO = mockk<UtbetalingerDAO>()

        every { accessClient.hasAccess(personIdent, "test-token", "call-id") } returns true
        every { utbetalingerDAO.fetchMaksDatoByFnr(personIdent) } returns null

        val mockMvc =
            standaloneSetup(
                SykepengerMaxDateAzureApiV1(
                    veilederTilgangskontrollClient = accessClient,
                    utbetalingerDAO = utbetalingerDAO,
                    metric = mockk(relaxed = true),
                ),
            ).build()

        val result =
            mockMvc
                .perform(azureRequest("/api/azure/v1/sykepenger/maxdate"))
                .andExpect(request().asyncStarted())
                .andReturn()

        mockMvc
            .perform(asyncDispatch(result))
            .andExpect(status().isOk)
            .andExpect(content().json("""{"maxDate":null}""", JsonCompareMode.STRICT))
    }

    @Test
    fun `Azure v2 serializes nested PMaksDato with nullable utbetalt tom`() {
        val personIdent = "12121212121"
        val accessClient = mockk<VeilederTilgangskontrollClient>()
        val utbetalingerDAO = mockk<UtbetalingerDAO>()
        val tokenValidationContextHolder = mockk<TokenValidationContextHolder>()

        every { accessClient.hasAccess(personIdent, "test-token", "call-id") } returns true
        every { utbetalingerDAO.fetchMaksDatoByFnr(personIdent) } returns
            maksDato(personIdent).copy(utbetalt_tom = null)

        mockkObject(TokenUtil)
        every { TokenUtil.getIssuerToken(tokenValidationContextHolder, "azuread") } returns "test-token"

        try {
            val mockMvc =
                standaloneSetup(
                    SykepengerMaxDateAzureApiV2(
                        veilederTilgangskontrollClient = accessClient,
                        utbetalingerDAO = utbetalingerDAO,
                        metric = mockk(relaxed = true),
                        tokenValidationContextHolder = tokenValidationContextHolder,
                    ),
                ).build()

            val result =
                mockMvc
                    .perform(azureRequest("/api/azure/v2/sykepenger/maxdate"))
                    .andExpect(request().asyncStarted())
                    .andReturn()

            mockMvc
                .perform(asyncDispatch(result))
                .andExpect(status().isOk)
                .andExpect(
                    content().json(
                        """
                        {
                          "maxDate": {
                            "id": "maksdato-id",
                            "fnr": "12121212121",
                            "forelopig_beregnet_slutt": "2026-09-30",
                            "utbetalt_tom": null,
                            "tom": "2026-09-20",
                            "gjenstaende_sykedager": "30",
                            "opprettet": "2026-08-29T08:00:00"
                          }
                        }
                        """.trimIndent(),
                        JsonCompareMode.STRICT,
                    ),
                )
        } finally {
            unmockkObject(TokenUtil)
        }
    }

    @Test
    fun `global exception handler serializes reason with error status`() {
        val mockMvc =
            standaloneSetup(ContractErrorController())
                .setControllerAdvice(GlobalExceptionHandler())
                .build()

        mockMvc
            .perform(get("/contract-error"))
            .andExpect(status().isBadRequest)
            .andExpect(
                content().json(
                    """{"reason":"Invalid contract request"}""",
                    JsonCompareMode.STRICT,
                ),
            )
    }

    private fun maksDato(personIdent: String) = PMaksDato(
        id = "maksdato-id",
        fnr = personIdent,
        forelopig_beregnet_slutt = LocalDate.of(2026, 9, 30),
        utbetalt_tom = LocalDate.of(2026, 9, 20),
        tom = LocalDate.of(2026, 9, 20),
        gjenstaende_sykedager = "30",
        opprettet = LocalDateTime.of(2026, 8, 29, 8, 0),
    )

    private fun azureRequest(path: String) = get(path)
        .header(NAV_PERSONIDENT_HEADER, "12121212121")
        .header(NAV_CALL_ID_HEADER, "call-id")
        .header("authorization", "test-token")

    private fun standaloneSetup(vararg controllers: Any): StandaloneMockMvcBuilder = MockMvcBuilders
        .standaloneSetup(*controllers)
        .setMessageConverters(MappingJackson2HttpMessageConverter(jacksonMapper()))
}

@RestController
private class ContractErrorController {
    @GetMapping("/contract-error")
    fun error(): Nothing = throw ContractApiError()
}

private class ContractApiError :
    AbstractApiError(
        message = "Invalid contract request",
        httpStatus = HttpStatus.BAD_REQUEST,
        reason = "Invalid contract request",
        loglevel = LogLevel.OFF,
    )
