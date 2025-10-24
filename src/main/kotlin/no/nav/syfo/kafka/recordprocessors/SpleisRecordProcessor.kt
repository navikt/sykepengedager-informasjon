package no.nav.syfo.kafka.recordprocessors

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.db.UtbetalingSpleisDAO
import no.nav.syfo.kafka.consumers.spleis.domain.DagType
import no.nav.syfo.kafka.consumers.spleis.domain.UTBETALING_UTBETALT
import no.nav.syfo.kafka.consumers.spleis.domain.UTBETALING_UTEN_UTBETALING
import no.nav.syfo.kafka.consumers.spleis.domain.UtbetalingSpleis
import no.nav.syfo.kafka.producers.SykepengedagerInformasjonKafkaService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class SpleisRecordProcessor(
    val sykepengedagerInformasjonKafkaService: SykepengedagerInformasjonKafkaService,
    private val objectMapper: ObjectMapper,
) {
    private val sykepengedagtyper = listOf(DagType.NavDag, DagType.NavHelgDag, DagType.ArbeidsgiverperiodeDag)

    @Autowired
    private lateinit var utbetalingSpleisDAO: UtbetalingSpleisDAO

    fun processRecord(record: ConsumerRecord<String, String>) {
        val utbetaling = objectMapper.readValue(record.value(), UtbetalingSpleis::class.java)
        if (utbetaling.event == UTBETALING_UTBETALT || utbetaling.event == UTBETALING_UTEN_UTBETALING) {
            processUtbetalingSpleisEvent(utbetaling)
        }
    }
    fun processUtbetalingSpleisEvent(utbetaling: UtbetalingSpleis) {
        val utbetaltTom = calculateUtbetaltTom(utbetaling)
        utbetalingSpleisDAO.storeSpleisUtbetaling(utbetaling, utbetaltTom)
        sykepengedagerInformasjonKafkaService.publishSykepengedagerInformasjonEvent(utbetaling.fødselsnummer)
    }

    private fun calculateUtbetaltTom(utbetaling: UtbetalingSpleis): LocalDate? {
        return if (utbetaling.event == UTBETALING_UTBETALT) {
            utbetaling.utbetalingsdager.filter { it.type in sykepengedagtyper }.maxOfOrNull { it.dato }
        } else null
    }
}
