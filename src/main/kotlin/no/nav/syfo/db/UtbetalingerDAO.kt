package no.nav.syfo.db

import no.nav.syfo.metric.Metric
import no.nav.syfo.metric.TimerBuilderName
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class UtbetalingerDAO(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val metric: Metric,
) {
    fun fetchMaksDatoByFnr(fnr: String): PMaksDato? {
        val queryStatement =
            """
            SELECT *
            FROM UTBETALINGER AS UTBETALINGER1
            WHERE UUID =
                (SELECT UTBETALINGER2.UUID
                FROM UTBETALINGER AS UTBETALINGER2
                WHERE UTBETALINGER1.FNR = UTBETALINGER2.FNR
                ORDER BY OPPRETTET DESC
                LIMIT 1)
            AND FNR = :FNR
            """.trimIndent()

        val timer = metric.createTimer("utbetalinger_view", TimerBuilderName.DATABASE_QUERY_LATENCY.name)

        return timer.record<PMaksDato> {
            val mapQueryStatement =
                MapSqlParameterSource()
                    .addValue("FNR", fnr)

            val resultList =
                try {
                    namedParameterJdbcTemplate.query(queryStatement, mapQueryStatement, MaxDateRowMapper())
                } catch (e: EmptyResultDataAccessException) {
                    emptyList()
                }

            return@record if (resultList.isNotEmpty()) {
                resultList.first()
            } else {
                null
            }
        }
    }
}

private class MaxDateRowMapper : RowMapper<PMaksDato> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): PMaksDato =
        PMaksDato(
            id = rs.getString("UUID"),
            fnr = rs.getString("FNR"),
            forelopig_beregnet_slutt = rs.getTimestamp("FORELOPIG_BEREGNET_SLUTT").toLocalDateTime().toLocalDate(),
            utbetalt_tom = rs.getTimestamp("UTBETALT_TOM").toLocalDateTime().toLocalDate(),
            gjenstaende_sykedager = rs.getString("GJENSTAENDE_SYKEDAGER"),
            opprettet = rs.getTimestamp("OPPRETTET").toLocalDateTime(),
        )
}
