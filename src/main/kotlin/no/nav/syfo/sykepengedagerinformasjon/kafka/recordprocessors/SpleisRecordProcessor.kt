package no.nav.syfo.sykepengedagerinformasjon.kafka.recordprocessors

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.syfo.sykepengedagerinformasjon.config.kafka.topicUtbetaling
import no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.spleis.domain.UTBETALING_UTBETALT
import no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.spleis.domain.UTBETALING_UTEN_UTBETALING
import no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.spleis.domain.UtbetalingSpleis
import no.nav.syfo.sykepengedagerinformasjon.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Component

@Component
class SpleisRecordProcessor {
    private val log = logger()
    private val objectMapper = jacksonObjectMapper()

    @Suppress("TooGenericExceptionCaught")
    fun processRecord(record: ConsumerRecord<String, String>) {
        try {
            log.info("TODO: processing Spleis: start")
            log.info("TODO: processing Spleis value: ${record.value()}")
            val utbetaling = objectMapper.readValue(record.value(), UtbetalingSpleis::class.java)
            if (utbetaling.event == UTBETALING_UTBETALT || utbetaling.event == UTBETALING_UTEN_UTBETALING) {
                log.info("TODO: About to process ${utbetaling.event}")
                processUtbetalingSpleisEvent(utbetaling)
            }
        } catch (e: Exception) {
            log.error("Exception in [$topicUtbetaling]-processor: $e", e)
        }
    }

    private fun processUtbetalingSpleisEvent(utbetaling: UtbetalingSpleis) {
        val fnr = utbetaling.fødselsnummer
        log.info("TODO: processing fnr")
        // processFodselsdato(fnr)
        // databaseInterface.storeSpleisUtbetaling(utbetaling)
    }
}
