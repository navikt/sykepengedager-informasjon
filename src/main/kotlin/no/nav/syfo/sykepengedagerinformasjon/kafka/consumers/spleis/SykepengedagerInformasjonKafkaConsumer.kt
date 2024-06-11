package no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.spleis

import no.nav.syfo.sykepengedagerinformasjon.config.kafka.topicSykepengedagerInfotrygd
import no.nav.syfo.sykepengedagerinformasjon.config.kafka.topicUtbetaling
import no.nav.syfo.sykepengedagerinformasjon.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Suppress("TooGenericExceptionCaught")
@Component
class SykepengedagerInformasjonKafkaConsumer {
    private val log = logger()

    @KafkaListener(
        topics = [topicUtbetaling, topicSykepengedagerInfotrygd],
    )
    fun listen(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        val topic = record.topic()
        try {
            log.info(
                "Todo: received e record from topic $topic",
            )

            when (topic) {
                topicUtbetaling -> log.info("Todo: should process record from topicUtbetaling $topicUtbetaling")
                topicSykepengedagerInfotrygd ->
                    log.info(
                        "Todo: should process record from topicSykepengedagerInfotrygd $topicSykepengedagerInfotrygd"
                    )
            }
            ack.acknowledge()
        } catch (e: Exception) {
            log.error("Exception in ${SykepengedagerInformasjonKafkaConsumer::class.qualifiedName}-listener: $e", e)
        }
    }
}
