package no.nav.syfo.api

import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.consumer.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.db.PMaksDato
import no.nav.syfo.db.UtbetalingerDAO
import no.nav.syfo.metric.Metric
import no.nav.syfo.utils.NAV_CALL_ID_HEADER
import no.nav.syfo.utils.NAV_PERSONIDENT_HEADER
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate
import java.time.LocalDateTime

class SpringSuspendEndpointIntegrationTest {
    @Test
    fun `suspend endpoint completes through Spring MVC async dispatch`() {
        val personIdent = "12121212121"
        val accessClient = mockk<VeilederTilgangskontrollClient>()
        val utbetalingerDAO = mockk<UtbetalingerDAO>()
        val metric = mockk<Metric>(relaxed = true)

        every { accessClient.hasAccess(personIdent, "test-token", "call-id") } returns true
        every { utbetalingerDAO.fetchMaksDatoByFnr(personIdent) } returns
            PMaksDato(
                id = "maksdato-id",
                fnr = personIdent,
                forelopig_beregnet_slutt = LocalDate.of(2026, 9, 30),
                utbetalt_tom = LocalDate.of(2026, 9, 20),
                tom = LocalDate.of(2026, 9, 20),
                gjenstaende_sykedager = "30",
                opprettet = LocalDateTime.of(2026, 8, 29, 8, 0),
            )

        val mockMvc =
            MockMvcBuilders
                .standaloneSetup(
                    SykepengerMaxDateAzureApiV1(
                        veilederTilgangskontrollClient = accessClient,
                        utbetalingerDAO = utbetalingerDAO,
                        metric = metric,
                    ),
                ).build()

        val asyncResult =
            mockMvc
                .perform(
                    get("/api/azure/v1/sykepenger/maxdate")
                        .header(NAV_PERSONIDENT_HEADER, personIdent)
                        .header(NAV_CALL_ID_HEADER, "call-id")
                        .header("authorization", "Bearer test-token"),
                ).andExpect(request().asyncStarted())
                .andReturn()

        mockMvc
            .perform(asyncDispatch(asyncResult))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.maxDate").exists())
    }
}
