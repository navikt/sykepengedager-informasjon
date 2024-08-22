package no.nav.syfo.kafka.consumers.spleis

import no.nav.syfo.config.kafka.topicSykepengedagerInfotrygd
import no.nav.syfo.config.kafka.topicUtbetaling
import no.nav.syfo.kafka.recordprocessors.InfotrygdRecordProcessor
import no.nav.syfo.kafka.recordprocessors.SpleisRecordProcessor
import no.nav.syfo.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
@Profile("remote")
class SykepengedagerInformasjonKafkaConsumer(
    private val spleisRecordProcessor: SpleisRecordProcessor,
    private val infotrygdRecordProcessor: InfotrygdRecordProcessor,
) {
    private val log = logger()

    @KafkaListener(
        topics = [topicUtbetaling, topicSykepengedagerInfotrygd],
        autoStartup = "true" // Enable consuming
    )
    fun listen(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        try {
            val topic = record.topic()
            log.info(
                "Received e record from topic $topic",
            )

            when (topic) {
                topicUtbetaling -> {
                    log.info("Going to process record from topicUtbetaling $topic")
                    spleisRecordProcessor.processRecord(record)
                }

                topicSykepengedagerInfotrygd -> {
                    log.info(
                        "Going to process record from topicSykepengedagerInfotrygd $topic",
                    )
                    infotrygdRecordProcessor.processRecord(record)
                }
            }
            ack.acknowledge()
        } catch (e: Exception) {
            log.error("Exception in ${SykepengedagerInformasjonKafkaConsumer::class.qualifiedName}-listener: $e", e)
        }
    }
}
