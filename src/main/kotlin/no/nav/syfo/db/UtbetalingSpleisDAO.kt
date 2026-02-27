package no.nav.syfo.db

import no.nav.syfo.kafka.consumers.spleis.domain.UtbetalingSpleis
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Repository
class UtbetalingSpleisDAO(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,) {
    fun storeSpleisUtbetaling(utbetaling: UtbetalingSpleis, utbetaltTom: LocalDate?): UUID {
        val sql =
            """
             INSERT INTO UTBETALING_SPLEIS  (
            UUID, 
            FNR, 
            ORGANISASJONSNUMMER, 
            EVENT, 
            TYPE,
            FORELOPIG_BEREGNET_SLUTT,
            FORBRUKTE_SYKEDAGER,
            GJENSTAENDE_SYKEDAGER,
            STONADSDAGER,
            ANTALL_VEDTAK,
            FOM,
            TOM,
            UTBETALING_ID,
            KORRELASJON_ID,
            OPPRETTET,
            UTBETALT_TOM)  
            
            VALUES (
            :UUID, 
            :FNR, 
            :ORGANISASJONSNUMMER, 
            :EVENT, 
            :TYPE,
            :FORELOPIG_BEREGNET_SLUTT,
            :FORBRUKTE_SYKEDAGER,
            :GJENSTAENDE_SYKEDAGER,
            :STONADSDAGER,
            :ANTALL_VEDTAK,
            :FOM,
            :TOM,
            :UTBETALING_ID,
            :KORRELASJON_ID,
            :OPPRETTET,
            :UTBETALT_TOM)
            """.trimIndent()

        val uuid = UUID.randomUUID()

        val params =
            MapSqlParameterSource()
                .addValue("UUID", uuid)
                .addValue("FNR", utbetaling.fødselsnummer)
                .addValue("ORGANISASJONSNUMMER", utbetaling.organisasjonsnummer)
                .addValue("EVENT", utbetaling.event)
                .addValue("TYPE", utbetaling.type)
                .addValue("FORELOPIG_BEREGNET_SLUTT", Date.valueOf(utbetaling.foreløpigBeregnetSluttPåSykepenger))
                .addValue(
                    "FORBRUKTE_SYKEDAGER",
                    utbetaling.forbrukteSykedager!!,
                ) // Ikke nullable for event utbetaling_utbetalt
                .addValue(
                    "GJENSTAENDE_SYKEDAGER",
                    utbetaling.gjenståendeSykedager!!,
                ) // Ikke nullable for event utbetaling_utbetalt
                .addValue("STONADSDAGER", utbetaling.stønadsdager!!) // Ikke nullable for event utbetaling_utbetalt
                .addValue("ANTALL_VEDTAK", utbetaling.antallVedtak!!) // Ikke nullable for event utbetaling_utbetalt
                .addValue("FOM", Date.valueOf(utbetaling.fom))
                .addValue("TOM", Date.valueOf(utbetaling.tom))
                .addValue("UTBETALING_ID", utbetaling.utbetalingId)
                .addValue("KORRELASJON_ID", utbetaling.korrelasjonsId)
                .addValue("OPPRETTET", Timestamp.valueOf(LocalDateTime.now()))
                .addValue("UTBETALT_TOM", utbetaltTom?.let { Date.valueOf(it) })

        namedParameterJdbcTemplate.update(sql, params)

        return uuid
    }

    // Test only
    fun fetchSpleisUtbetalingByFnr(fnr: String): UtbetalingSpleis? {
        val queryStatement =
            """
            SELECT *
            FROM UTBETALING_SPLEIS
            WHERE  FNR = :FNR
            """.trimIndent()

        val mapQueryStatement =
            MapSqlParameterSource()
                .addValue("FNR", fnr)

        val resultList =
            try {
                namedParameterJdbcTemplate.query(queryStatement, mapQueryStatement, UtbetalingSpleisRowMapper())
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

private class UtbetalingSpleisRowMapper : RowMapper<UtbetalingSpleis> {
    override fun mapRow(rs: ResultSet, rowNum: Int,): UtbetalingSpleis = UtbetalingSpleis(
        fødselsnummer = rs.getString("FNR"),
        organisasjonsnummer = rs.getString("ORGANISASJONSNUMMER"),
        event = rs.getString("EVENT"),
        type = rs.getString("TYPE"),
        fom = rs.getDate("FOM").toString(),
        tom = rs.getDate("TOM").toString(),
        foreløpigBeregnetSluttPåSykepenger = rs.getDate("FORELOPIG_BEREGNET_SLUTT").toString(),
        forbrukteSykedager = rs.getInt("FORBRUKTE_SYKEDAGER"),
        gjenståendeSykedager = rs.getInt("GJENSTAENDE_SYKEDAGER"),
        stønadsdager = rs.getInt("STONADSDAGER"),
        antallVedtak = rs.getInt("ANTALL_VEDTAK"),
        utbetalingId = rs.getString("UTBETALING_ID"),
        korrelasjonsId = rs.getString("KORRELASJON_ID"),
        utbetalingsdager = emptyList(),
    )
}
