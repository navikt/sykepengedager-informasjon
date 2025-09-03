package no.nav.syfo.kafka.recordprocessors

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.syfo.db.UtbetalingInfotrygdDAO
import no.nav.syfo.kafka.consumers.aapInfotrygd.domain.InfotrygdSource
import no.nav.syfo.kafka.consumers.aapInfotrygd.domain.KInfotrygdSykepengedager
import no.nav.syfo.kafka.consumers.aapInfotrygd.gjenstaendeSykepengedager
import no.nav.syfo.kafka.producers.SykepengedagerInformasjonKafkaService
import no.nav.syfo.logger
import no.nav.syfo.utils.parseDate
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class InfotrygdRecordProcessor(
    val sykepengedagerInformasjonKafkaService: SykepengedagerInformasjonKafkaService,
) {
    private val log = logger()
    private val objectMapper = jacksonObjectMapper()

    @Autowired
    private lateinit var utbetalingInfotrygdDAO: UtbetalingInfotrygdDAO

    fun processRecord(record: ConsumerRecord<String, String>) {
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
    }

    private fun processInfotrygdEvent(
        fnr: String,
        sykepengerMaxDate: LocalDate,
        utbetaltTilDate: LocalDate,
        gjenstaendeSykepengedager: Int,
        source: InfotrygdSource,
    ) {
        if (!utbetalingInfotrygdDAO.isInfotrygdUtbetalingExists(fnr, sykepengerMaxDate, utbetaltTilDate)) {
            utbetalingInfotrygdDAO.storeInfotrygdUtbetaling(
                fnr,
                sykepengerMaxDate,
                utbetaltTilDate,
                gjenstaendeSykepengedager,
                source,
            )
            sykepengedagerInformasjonKafkaService.publishSykepengedagerInformasjonEvent(fnr)
        } else {
            log.info("Infotrygd utbetaling with the same fnr, sykepengerMaxDate and  utbetaltTilDate already exists")
        }
    }
}
