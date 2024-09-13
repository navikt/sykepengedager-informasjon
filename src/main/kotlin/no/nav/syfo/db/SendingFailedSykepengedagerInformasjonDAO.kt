package no.nav.syfo.db

import no.nav.syfo.kafka.producers.domain.KSykepengedagerInformasjonDTO
import no.nav.syfo.logger
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

@Repository
class SendingFailedSykepengedagerInformasjonDAO(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,) {
    private val log = logger()

    fun storeSendingFailedSykepengedagerInformasjon(
        event: KSykepengedagerInformasjonDTO,
        errorMessage: String,
        sendingOutTimestamp: LocalDateTime,
    ): UUID? {
        val sql =
            """
            INSERT INTO SENDING_FEILED_SYKEPENGEDAGER_INFORMASJON  (
            UUID, 
            EVENT_ID,
            CREATED_AT,
            ERROR_MESSAGE) 
            
            VALUES (
            :UUID, 
            :EVENT_ID,
            :CREATED_AT,
            :ERROR_MESSAGE)
            """.trimIndent()

        val uuid = UUID.randomUUID()
        val params =
            MapSqlParameterSource()
                .addValue("UUID", uuid)
                .addValue("EVENT_ID", event.id) // UUID of the original record
                .addValue("CREATED_AT", Timestamp.valueOf(sendingOutTimestamp))
                .addValue("ERROR_MESSAGE", errorMessage)
        try {
            namedParameterJdbcTemplate.update(sql, params)
            return uuid
        } catch (e: Exception) {
            log.error(
                "Could not execute insert in SENDING_FEILED_SYKEPENGEDAGER_INFORMASJON, error message: ${e.message}"
            )
            return null
        }
    }

    fun fetchSendingFailedSykepengedagerInformasjonByFnr(eventId: String): PSykepengedagerInformasjonSentStatus? {
        val queryStatement =
            """
            SELECT *
            FROM SENDING_FEILED_SYKEPENGEDAGER_INFORMASJON
            WHERE  EVENT_ID = :EVENT_ID
            """.trimIndent()

        val mapQueryStatement =
            MapSqlParameterSource()
                .addValue("EVENT_ID", eventId)

        val resultList =
            try {
                namedParameterJdbcTemplate.query(queryStatement, mapQueryStatement) { rs, _ ->
                    PSykepengedagerInformasjonSentStatus(
                        rs.getString("UUID"),
                        rs.getString("EVENT_ID"),
                        rs.getDate("CREATED_AT").toLocalDate(),
                        rs.getString("ERROR_MESSAGE"),
                    )
                }
            } catch (e: EmptyResultDataAccessException) {
                emptyList()
            }

        return if (resultList.isNotEmpty()) {
            resultList.first()
        } else {
            null
        }
    }
}
