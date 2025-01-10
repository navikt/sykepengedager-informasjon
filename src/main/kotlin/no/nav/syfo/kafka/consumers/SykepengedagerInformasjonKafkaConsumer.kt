package no.nav.syfo.kafka.consumers

import no.nav.syfo.config.kafka.topicAapSykepengedagerInfotrygd
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
        topics = [topicUtbetaling],
        autoStartup = "true" // Enable consuming
    )
    fun listen(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        try {
            val topic = record.topic()
            log.info(
                "Received a record from topic $topicUtbetaling",
            )

            when (topic) {
                topicUtbetaling -> {
                    log.info("Going to process record from topicUtbetaling $topic")
                    spleisRecordProcessor.processRecord(record)
                }
            }
            ack.acknowledge()
        } catch (e: Exception) {
            log.error("Exception in ${SykepengedagerInformasjonKafkaConsumer::class.qualifiedName}-listener: $e", e)
        }
    }

    @KafkaListener(
        topics = [topicAapSykepengedagerInfotrygd],
        autoStartup = "true", // Enable consuming
        containerFactory = "infotrygdKafkaListenerContainerFactory",
    )
    fun listenAapTopicSykepengedagerInfotrygd(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        try {
            val topic = record.topic()
            log.info(
                "Received a record from topic $topicAapSykepengedagerInfotrygd",
            )

            when (topic) {
                topicAapSykepengedagerInfotrygd -> {
                    log.info(
                        "Going to process record from topicAapSykepengedagerInfotrygd $topic",
                    )
                    infotrygdRecordProcessor.processRecord(record)
                }
            }
            ack.acknowledge()
        } catch (e: Exception) {
            log.error("Exception in ${SykepengedagerInformasjonKafkaConsumer::class.qualifiedName}-listener: $e", e)
        }
    }

    @KafkaListener(
        topics = [topicSykepengedagerInfotrygd],
        autoStartup = "true", // Enable consuming
        containerFactory = "infotrygdKafkaListenerContainerFactory",
    )
    fun listenTopicSykepengedagerInfotrygd(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        try {
            log.info(
                "Received a record with key ${record.key()} from topic $topicSykepengedagerInfotrygd",
            )

            ack.acknowledge()
        } catch (e: Exception) {
            log.error("Exception in ${SykepengedagerInformasjonKafkaConsumer::class.qualifiedName}-listener: $e", e)
        }
    }
}
