package no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.spleis

import no.nav.syfo.sykepengedagerinformasjon.config.kafka.topicUtbetaling
import no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.spleis.domain.UtbetalingSpleis
import no.nav.syfo.sykepengedagerinformasjon.logger
import org.springframework.kafka.annotation.KafkaListener

@Suppress("TooGenericExceptionCaught")
class SpleisKafkaConsumer {
    private val log = logger()

    @KafkaListener(topics = [topicUtbetaling], containerFactory = "spleisKafkaListenerWithFilterContainerFactory")
    fun listen(utbetalingSpleis: UtbetalingSpleis) {
        log.info("Received record from spleis: $utbetalingSpleis")
        try {
            log.info(
                "Todo: should process utbetaling record ${utbetalingSpleis.utbetalingId}, ${utbetalingSpleis.event}",
            )
        } catch (e: Exception) {
            log.error("Exception in [$topicUtbetaling]-listener: $e", e)
        }
    }
}
