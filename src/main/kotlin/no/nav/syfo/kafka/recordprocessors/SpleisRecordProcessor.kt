package no.nav.syfo.kafka.recordprocessors

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.syfo.config.kafka.topicUtbetaling
import no.nav.syfo.db.UtbetalingSpleisDAO
import no.nav.syfo.kafka.consumers.spleis.domain.DagType
import no.nav.syfo.kafka.consumers.spleis.domain.UTBETALING_UTBETALT
import no.nav.syfo.kafka.consumers.spleis.domain.UTBETALING_UTEN_UTBETALING
import no.nav.syfo.kafka.consumers.spleis.domain.UtbetalingSpleis
import no.nav.syfo.kafka.producers.SykepengedagerInformasjonKafkaService
import no.nav.syfo.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class SpleisRecordProcessor(
    val sykepengedagerInformasjonKafkaService: SykepengedagerInformasjonKafkaService,
) {
    private val log = logger()
    private val objectMapper = jacksonObjectMapper()
    private val sykepengedagtyper = listOf(DagType.NavDag, DagType.NavHelgDag, DagType.ArbeidsgiverperiodeDag)

    @Autowired
    private lateinit var utbetalingSpleisDAO: UtbetalingSpleisDAO

    fun processRecord(record: ConsumerRecord<String, String>) {
        try {
            val utbetaling = objectMapper.readValue(record.value(), UtbetalingSpleis::class.java)
            if (utbetaling.event == UTBETALING_UTBETALT || utbetaling.event == UTBETALING_UTEN_UTBETALING) {
                processUtbetalingSpleisEvent(utbetaling)
            }
        } catch (e: Exception) {
            log.error("Exception in [$topicUtbetaling]-processor: $e", e)
        }
    }
    private fun processUtbetalingSpleisEvent(utbetaling: UtbetalingSpleis) {
        val utbetaltTom = calculateUtbetaltTom(utbetaling)
        utbetalingSpleisDAO.storeSpleisUtbetaling(utbetaling, utbetaltTom)
        if (utbetaltTom != LocalDate.parse(utbetaling.tom)) {
            log.info(
                "UtbetTom $utbetaltTom er forskjellig fra tom ${utbetaling.tom} i utbetaling med dager: " +
                    "${utbetaling.utbetalingsdager.joinToString()}"
            )
        }
        sykepengedagerInformasjonKafkaService.publishSykepengedagerInformasjonEvent(utbetaling.fødselsnummer)
    }

    private fun calculateUtbetaltTom(utbetaling: UtbetalingSpleis): LocalDate? =
        utbetaling.utbetalingsdager.filter { it.type in sykepengedagtyper }.maxOfOrNull { it.dato }
}
