package no.nav.syfo.config.kafka

import no.nav.syfo.LocalApplication
import no.nav.syfo.kafka.consumers.aapInfotrygd.domain.KInfotrygdSykepengedager
import no.nav.syfo.kafka.consumers.spleis.domain.DagType
import no.nav.syfo.kafka.consumers.spleis.domain.UtbetalingSpleis
import no.nav.syfo.kafka.consumers.spleis.domain.UtbetalingsdagDto
import no.nav.syfo.kafka.producers.domain.KSykepengedagerInformasjonDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(classes = [LocalApplication::class])
class JacksonKafkaContractTest {
    @Autowired
    private lateinit var objectMapper: JsonMapper

    @Test
    fun `serializes published sykepengedager information with camel case ISO fields`() {
        val event =
            KSykepengedagerInformasjonDTO(
                id = "event-123",
                personIdent = "synthetic-person-ident",
                forelopigBeregnetSlutt = LocalDate.parse("2026-10-31"),
                utbetaltTom = LocalDate.parse("2026-10-15"),
                gjenstaendeSykedager = "42",
                createdAt = LocalDateTime.parse("2026-09-16T14:10:16"),
            )

        val serialized = JacksonKafkaSerializer().serialize("sykepengedager", event)

        assertEquals(
            objectMapper.readTree(
                """
                {
                  "id": "event-123",
                  "personIdent": "synthetic-person-ident",
                  "forelopigBeregnetSlutt": "2026-10-31",
                  "utbetaltTom": "2026-10-15",
                  "gjenstaendeSykedager": "42",
                  "createdAt": "2026-09-16T14:10:16"
                }
                """.trimIndent(),
            ),
            objectMapper.readTree(serialized),
        )
    }

    @Test
    fun `deserializes Spleis Norwegian properties and uses Ukjent for an unknown day type`() {
        val actual =
            objectMapper.readValue(
                """
                {
                  "fødselsnummer": "synthetic-person-ident",
                  "organisasjonsnummer": "synthetic-organization-number",
                  "event": "utbetaling_utbetalt",
                  "type": "UTBETALING",
                  "foreløpigBeregnetSluttPåSykepenger": "2026-10-31",
                  "forbrukteSykedager": 10,
                  "gjenståendeSykedager": 42,
                  "stønadsdager": 52,
                  "antallVedtak": 2,
                  "fom": "2026-09-01",
                  "tom": "2026-09-30",
                  "utbetalingId": "utbetaling-123",
                  "korrelasjonsId": "korrelasjon-123",
                  "utbetalingsdager": [
                    {
                      "dato": "2026-09-16",
                      "type": "NewDagType",
                      "begrunnelser": ["UKJENT"]
                    }
                  ],
                  "unknownProperty": "accepted"
                }
                """.trimIndent(),
                UtbetalingSpleis::class.java,
            )

        assertEquals(
            UtbetalingSpleis(
                fødselsnummer = "synthetic-person-ident",
                organisasjonsnummer = "synthetic-organization-number",
                event = "utbetaling_utbetalt",
                type = "UTBETALING",
                foreløpigBeregnetSluttPåSykepenger = "2026-10-31",
                forbrukteSykedager = 10,
                gjenståendeSykedager = 42,
                stønadsdager = 52,
                antallVedtak = 2,
                fom = "2026-09-01",
                tom = "2026-09-30",
                utbetalingId = "utbetaling-123",
                korrelasjonsId = "korrelasjon-123",
                utbetalingsdager = listOf(
                    UtbetalingsdagDto(
                        dato = LocalDate.parse("2026-09-16"),
                        type = DagType.Ukjent,
                        begrunnelser = listOf("UKJENT"),
                    ),
                ),
            ),
            actual,
        )
    }

    @Test
    fun `deserializes Infotrygd uppercase properties with nullable UTBET TOM`() {
        val actual =
            objectMapper.readValue(
                """
                {
                  "after": {
                    "MAX_DATO": "2026-10-31",
                    "UTBET_TOM": null,
                    "F_NR": "synthetic-person-ident",
                    "UNKNOWN_PROPERTY": "accepted"
                  },
                  "unknownProperty": "accepted"
                }
                """.trimIndent(),
                KInfotrygdSykepengedager::class.java,
            )

        assertEquals(
            KInfotrygdSykepengedager.After(
                MAX_DATO = "2026-10-31",
                UTBET_TOM = null,
                F_NR = "synthetic-person-ident",
            ),
            actual.after,
        )
    }
}
