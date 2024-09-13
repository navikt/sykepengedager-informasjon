package no.nav.syfo.db

import no.nav.syfo.kafka.producers.domain.KSykepengedagerInformasjonDTO
import no.nav.syfo.logger
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

@Repository
class SentSykepengedagerInformasjonDAO(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,) {
    private val log = logger()

    fun storeSentSykepengedagerInformasjon(
        event: KSykepengedagerInformasjonDTO,
        sendingOutTimestamp: LocalDateTime,
    ): UUID? {
        val sql =
            """
            INSERT INTO SENT_SYKEPENGEDAGER_INFORMASJON  (
            UUID, 
            EVENT_ID,
            CREATED_AT) 
            
            VALUES (
            :UUID, 
            :EVENT_ID,
            :CREATED_AT)
            """.trimIndent()

        val uuid = UUID.randomUUID()
        val params =
            MapSqlParameterSource()
                .addValue("UUID", uuid)
                .addValue("EVENT_ID", event.id) // UUID of the original record
                .addValue("CREATED_AT", Timestamp.valueOf(sendingOutTimestamp))
        try {
            namedParameterJdbcTemplate.update(sql, params)
            return uuid
        } catch (e: Exception) {
            log.error("Could not execute insert in SENT_SYKEPENGEDAGER_INFORMASJON, error message: ${e.message}")
            return null
        }
    }
}
