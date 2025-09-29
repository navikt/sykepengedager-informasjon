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
        val pMaksDato = maksDato(fnr)
        val utbetaltTom = utbetaling(fnr)
        return if (utbetaltTom?.utbetalt_tom == null) {
            pMaksDato
        } else {
            pMaksDato?.copy(utbetalt_tom = utbetaltTom.utbetalt_tom)
        }
    }

    private fun maksDato(fnr: String): PMaksDato? {
        val queryStatement =
            """
            SELECT *
            FROM MAXDATO
            WHERE FNR = :FNR
            ORDER BY TOM DESC, OPPRETTET DESC
            LIMIT 1
            """.trimIndent()

        val timer = metric.createTimer("maxdato_view", TimerBuilderName.DATABASE_QUERY_LATENCY.name)

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
            resultList.firstOrNull()
        }
    }

    private fun utbetaling(fnr: String): PMaksDato? {
        val queryStatement =
            """
            SELECT *
            FROM UTBETALING
            WHERE FNR = :FNR
            ORDER BY UTBETALT_TOM DESC, OPPRETTET DESC
            LIMIT 1
            """.trimIndent()

        val timer = metric.createTimer("utbetaling_view", TimerBuilderName.DATABASE_QUERY_LATENCY.name)

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
            resultList.firstOrNull()
        }
    }

    fun fetchMaksDatoByFnrForKafka(fnr: String): PMaksDato? {
        val queryStatement =
            """
            SELECT *
            FROM MAXDATO
            AND FNR = :FNR
            ORDER BY OPPRETTET DESC
            LIMIT 1
            """.trimIndent()

        val timer = metric.createTimer("utbetalinger_view_kafka", TimerBuilderName.DATABASE_QUERY_LATENCY.name)

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
            resultList.firstOrNull()
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
            utbetalt_tom = rs.getTimestamp("UTBETALT_TOM")?.toLocalDateTime()?.toLocalDate(),
            tom = rs.getTimestamp("TOM").toLocalDateTime().toLocalDate(),
            gjenstaende_sykedager = rs.getString("GJENSTAENDE_SYKEDAGER"),
            opprettet = rs.getTimestamp("OPPRETTET").toLocalDateTime(),
        )
}
