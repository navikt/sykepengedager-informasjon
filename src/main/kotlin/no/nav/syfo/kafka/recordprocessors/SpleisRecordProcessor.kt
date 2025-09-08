package no.nav.syfo.kafka.recordprocessors

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.syfo.db.UtbetalingSpleisDAO
import no.nav.syfo.kafka.consumers.spleis.domain.UTBETALING_UTBETALT
import no.nav.syfo.kafka.consumers.spleis.domain.UTBETALING_UTEN_UTBETALING
import no.nav.syfo.kafka.consumers.spleis.domain.UtbetalingSpleis
import no.nav.syfo.kafka.producers.SykepengedagerInformasjonKafkaService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SpleisRecordProcessor(
    val sykepengedagerInformasjonKafkaService: SykepengedagerInformasjonKafkaService,
) {
    private val objectMapper = jacksonObjectMapper()

    @Autowired
    private lateinit var utbetalingSpleisDAO: UtbetalingSpleisDAO

    @Transactional
    fun processRecord(record: ConsumerRecord<String, String>) {
        val utbetaling = objectMapper.readValue(record.value(), UtbetalingSpleis::class.java)
        if (utbetaling.event == UTBETALING_UTBETALT || utbetaling.event == UTBETALING_UTEN_UTBETALING) {
            processUtbetalingSpleisEvent(utbetaling)
        }
    }

    private fun processUtbetalingSpleisEvent(utbetaling: UtbetalingSpleis) {
        utbetalingSpleisDAO.storeSpleisUtbetaling(utbetaling)
        sykepengedagerInformasjonKafkaService.publishSykepengedagerInformasjonEvent(utbetaling.fødselsnummer)
    }
}
