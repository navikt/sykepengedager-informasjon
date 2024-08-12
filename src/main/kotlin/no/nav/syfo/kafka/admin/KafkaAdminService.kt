package no.nav.syfo.kafka.admin

import no.nav.syfo.config.kafka.AivenKafkaConfig
import no.nav.syfo.config.kafka.topicSykepengedagerInfotrygd
import no.nav.syfo.config.kafka.topicUtbetaling
import no.nav.syfo.logger
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.OffsetSpec
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.TopicPartition
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@Profile("remote")
class KafkaAdminService(
    private val kafkaSykepengedagerInformasjonConsumer: Consumer<String, String>,
    private val kafkaAdmin: KafkaAdmin,
    avienKafkaConfig: AivenKafkaConfig,
) {
    private val log = logger()
    private val topicInfotrygd = topicSykepengedagerInfotrygd
    private val topicSpleis = topicUtbetaling

    val commonConfig = avienKafkaConfig.commonConfig()

    @Scheduled(fixedRate = 60 * 60 * 1000) // Runs every 60 minutes
    fun run() {
        val adminClient = AdminClient.create(kafkaAdmin.configurationProperties + commonConfig)

        for (topic in listOf(topicInfotrygd, topicSpleis)) {
            log.info("[MAX_DATE_RECORDS] Start to count remaining records to consume from $topic")
            adminClient.use { client ->
                val partitions = kafkaSykepengedagerInformasjonConsumer.partitionsFor(topic)
                    .map { TopicPartition(it.topic(), it.partition()) }
                    .toSet()

                val currentOffsets = kafkaSykepengedagerInformasjonConsumer.committed(partitions)
                val endOffsets = client.listOffsets(
                    partitions.associateWith { OffsetSpec.latest() }
                ).all().get()

                var totalRecordsToConsume = 0L
                var totalUnconsumedRecords = 0L

                partitions.forEach { partition ->
                    val currentOffset = currentOffsets[partition]?.offset() ?: 0
                    val endOffset = endOffsets[partition]?.offset() ?: 0
                    val recordsToConsume = endOffset - currentOffset
                    totalRecordsToConsume += recordsToConsume
                    totalUnconsumedRecords += if (recordsToConsume > 0) recordsToConsume else 0
                }

                log.info("[MAX_DATE_RECORDS] Total partitions: ${partitions.size}")
                log.info("[MAX_DATE_RECORDS] Total records to consume: $totalRecordsToConsume")
                log.info("[MAX_DATE_RECORDS] Total unconsumed records: $totalUnconsumedRecords")

                val allConsumed = totalRecordsToConsume == 0L

                if (allConsumed) {
                    log.info("[MAX_DATE_RECORDS] All data from topic $topic is consumed.")
                } else {
                    log.info(
                        "[MAX_DATE_RECORDS] There are remaining $totalRecordsToConsume of $totalUnconsumedRecords " +
                            "records  from topic $topic to consume"
                    )
                }
            }
        }
    }
}
