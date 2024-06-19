package no.nav.syfo.sykepengedagerinformasjon.db

import no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.infotrygd.domain.InfotrygdSource
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Repository
class UtbetalingInfotrygdDAO(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun storeInfotrygdUtbetaling(
        fnr: String,
        sykepengerMaxDate: LocalDate,
        utbetaltTilDate: LocalDate,
        gjenstaendeSykepengedager: Int,
        source: InfotrygdSource,
    ): UUID {
        val sql =
            """
            INSERT INTO UTBETALING_INFOTRYGD  (
            UUID, 
            FNR, 
            MAX_DATE, 
            UTBET_TOM,
            GJENSTAENDE_SYKEDAGER,
            OPPRETTET,
            SOURCE) 
            
            VALUES (
            :UUID, 
            :FNR, 
            :MAX_DATE, 
            :UTBET_TOM,
            :GJENSTAENDE_SYKEDAGER,
            :OPPRETTET,
            :SOURCE)
            """.trimIndent()

        val uuid = UUID.randomUUID()
        val params =
            MapSqlParameterSource()
                .addValue("UUID", uuid)
                .addValue("FNR", fnr)
                .addValue("MAX_DATE", Date.valueOf(sykepengerMaxDate))
                .addValue("UTBET_TOM", Date.valueOf(utbetaltTilDate))
                .addValue("GJENSTAENDE_SYKEDAGER", gjenstaendeSykepengedager)
                .addValue("OPPRETTET", Timestamp.valueOf(LocalDateTime.now()))
                .addValue("SOURCE", source.name)

        namedParameterJdbcTemplate.update(sql, params)

        return uuid
    }

    fun fetchInfotrygdUtbetalingByFnr(fnr: String): Triple<String, String, Int>? {
        val queryStatement =
            """
            SELECT *
            FROM UTBETALING_INFOTRYGD
            WHERE  FNR = :FNR
            """.trimIndent()

        val mapQueryStatement =
            MapSqlParameterSource()
                .addValue("FNR", fnr)

        val resultList =
            try {
                namedParameterJdbcTemplate.query(queryStatement, mapQueryStatement) { rs, _ ->
                    Triple<String, String, Int>(
                        rs.getString("FNR"),
                        rs.getDate("MAX_DATE").toString(),
                        rs.getInt("GJENSTAENDE_SYKEDAGER"),
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
