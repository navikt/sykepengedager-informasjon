package no.nav.syfo.kafka.consumers

import no.nav.syfo.config.kafka.TOPIC_AAP_SYKEPENGEDAGER_INFOTRYGD
import no.nav.syfo.config.kafka.TOPIC_SYKEPENGEDAGER_INFOTRYGD
import no.nav.syfo.config.kafka.TOPIC_UTBETALING
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
        topics = [TOPIC_UTBETALING],
        autoStartup = "true" // Enable consuming
    )
    fun listen(record: ConsumerRecord<String, String>, ack: Acknowledgment,) {
        try {
            val topic = record.topic()
            log.info(
                "Received a record from topic $TOPIC_UTBETALING",
            )

            when (topic) {
                TOPIC_UTBETALING -> {
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
        topics = [TOPIC_AAP_SYKEPENGEDAGER_INFOTRYGD],
        autoStartup = "true", // Enable consuming
        containerFactory = "infotrygdKafkaListenerContainerFactory",
    )
    fun listenAapTopicSykepengedagerInfotrygd(record: ConsumerRecord<String, String>, ack: Acknowledgment,) {
        try {
            val topic = record.topic()
            log.info(
                "Received a record from topic $TOPIC_AAP_SYKEPENGEDAGER_INFOTRYGD",
            )

            when (topic) {
                TOPIC_AAP_SYKEPENGEDAGER_INFOTRYGD -> {
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
        topics = [TOPIC_SYKEPENGEDAGER_INFOTRYGD],
        autoStartup = "true", // Enable consuming
        containerFactory = "infotrygdKafkaListenerContainerFactory",
    )
    fun listenTopicSykepengedagerInfotrygd(record: ConsumerRecord<String, String>, ack: Acknowledgment,) {
        try {
            val topic = record.topic()
            log.info(
                "Received a record with key ${record.key()} from topic $TOPIC_SYKEPENGEDAGER_INFOTRYGD",
            )
            when (topic) {
                TOPIC_SYKEPENGEDAGER_INFOTRYGD -> {
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
