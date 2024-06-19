package no.nav.syfo.sykepengedagerinformasjon.kafka.recordprocessors

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.syfo.sykepengedagerinformasjon.config.kafka.topicSykepengedagerInfotrygd
import no.nav.syfo.sykepengedagerinformasjon.db.UtbetalingInfotrygdDAO
import no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.infotrygd.domain.InfotrygdSource
import no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.infotrygd.domain.KInfotrygdSykepengedager
import no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.infotrygd.gjenstaendeSykepengedager
import no.nav.syfo.sykepengedagerinformasjon.kafka.util.parseDate
import no.nav.syfo.sykepengedagerinformasjon.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class InfotrygdRecordProcessor {
    private val log = logger()
    private val objectMapper = jacksonObjectMapper()

    @Autowired
    private lateinit var utbetalingInfotrygdDAO: UtbetalingInfotrygdDAO

    fun processRecord(record: ConsumerRecord<String, String>) {
        try {
            val kInfotrygdSykepengedager = objectMapper.readValue(record.value(), KInfotrygdSykepengedager::class.java)
            val fnr = kInfotrygdSykepengedager.after.F_NR
            val sykepengerMaxDate = parseDate(kInfotrygdSykepengedager.after.MAX_DATO)
            val utbetaltTom = kInfotrygdSykepengedager.after.UTBET_TOM

            if (utbetaltTom != null) {
                val utbetaltTomDate = parseDate(utbetaltTom)

                processInfotrygdEvent(
                    fnr,
                    sykepengerMaxDate,
                    utbetaltTomDate,
                    utbetaltTomDate.gjenstaendeSykepengedager(sykepengerMaxDate),
                    InfotrygdSource.AAP_KAFKA_TOPIC,
                )
            }
        } catch (e: Exception) {
            log.error("Exception in [$topicSykepengedagerInfotrygd]-processor: $e", e)
        }
    }

    private fun processInfotrygdEvent(
        fnr: String,
        sykepengerMaxDate: LocalDate,
        utbetaltTilDate: LocalDate,
        gjenstaendeSykepengedager: Int,
        source: InfotrygdSource,
    ) {
        //        processFodselsdato(fnr) // TODO?
        utbetalingInfotrygdDAO.storeInfotrygdUtbetaling(
            fnr,
            sykepengerMaxDate,
            utbetaltTilDate,
            gjenstaendeSykepengedager,
            source,
        )
    }
}
