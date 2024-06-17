package no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.spleis

import no.nav.syfo.sykepengedagerinformasjon.config.kafka.topicSykepengedagerInfotrygd
import no.nav.syfo.sykepengedagerinformasjon.config.kafka.topicUtbetaling
import no.nav.syfo.sykepengedagerinformasjon.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Suppress("TooGenericExceptionCaught")
@Component
@Profile("remote")
class SykepengedagerInformasjonKafkaConsumer {
    private val log = logger()

    @KafkaListener(
        topics = [topicUtbetaling, topicSykepengedagerInfotrygd],
    )
    fun listen(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        try {
            val topic = record.topic()
            log.info(
                "Todo: received e record from topic $topic",
            )

            when (topic) {
                topicUtbetaling -> {
                    log.info("Todo: Going to process record from topicUtbetaling $topic")
                    //                    spleisRecordProcessor.processRecord(record)
                }

                topicSykepengedagerInfotrygd -> {
                    log.info(
                        "Todo: Going to process record from topicSykepengedagerInfotrygd $topic",
                    )
                    //                    infotrygdRecordProcessor.processRecord(record)
                }
            }
            ack.acknowledge()
        } catch (e: Exception) {
            log.error("Exception in ${SykepengedagerInformasjonKafkaConsumer::class.qualifiedName}-listener: $e", e)
        }
    }
}
