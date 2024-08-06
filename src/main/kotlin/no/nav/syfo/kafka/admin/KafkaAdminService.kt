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
import java.time.Duration

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

    @Scheduled(fixedRate = 60000) // Runs every 60 seconds
    fun run() {
        val adminClient = AdminClient.create(kafkaAdmin.configurationProperties + commonConfig)

        log.info("[MAX_DATE_RECORDS] Start to estimate remaining records to consume from $topicSpleis")
        adminClient.use { client ->
            val partitions = kafkaSykepengedagerInformasjonConsumer.partitionsFor(topicSpleis)
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

                log.info("[MAX_DATE_RECORDS] Partition: $partition, Records to consume: $recordsToConsume")

                totalRecordsToConsume += recordsToConsume
                totalUnconsumedRecords += if (recordsToConsume > 0) recordsToConsume else 0
            }

            log.info("[MAX_DATE_RECORDS] Total partitions: ${partitions.size}")
            log.info("[MAX_DATE_RECORDS] Total records to consume: $totalRecordsToConsume")
            log.info("[MAX_DATE_RECORDS] Total unconsumed records: $totalUnconsumedRecords")

            // Measure consumption rate
            val consumptionRate = measureConsumptionRate(topicSpleis, kafkaSykepengedagerInformasjonConsumer)

            // Estimate time to consume remaining records (in milliseconds)
            val estimatedTimeMillis = totalUnconsumedRecords / consumptionRate * 1000

            // Convert milliseconds to a human-readable format
            val estimatedTimeSeconds = estimatedTimeMillis / 1000
            val estimatedTimeMinutes = estimatedTimeSeconds / 60
            val estimatedTimeHours = estimatedTimeMinutes / 60

            log.info(
                "Estimated time to consume remaining records from topic $topicSpleis: $estimatedTimeHours hours"
            )

            val allConsumed = totalRecordsToConsume == 0L

            if (allConsumed) {
                log.info("[MAX_DATE_RECORDS] All data from topic $topicSpleis is consumed.")
            } else {
                log.info("[MAX_DATE_RECORDS] There is still data to be consumed from topic $topicSpleis.")
            }
        }

        log.info("[MAX_DATE_RECORDS] Start to estimate remaining records to consume from $topicInfotrygd")
        adminClient.use { client ->
            val partitions = kafkaSykepengedagerInformasjonConsumer.partitionsFor(topicInfotrygd)
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

                log.info("[MAX_DATE_RECORDS] Partition: $partition, Records to consume: $recordsToConsume")

                totalRecordsToConsume += recordsToConsume
                totalUnconsumedRecords += if (recordsToConsume > 0) recordsToConsume else 0
            }

            log.info("[MAX_DATE_RECORDS] Total partitions: ${partitions.size}")
            log.info("[MAX_DATE_RECORDS] Total records to consume: $totalRecordsToConsume")
            log.info("[MAX_DATE_RECORDS] Total unconsumed records: $totalUnconsumedRecords")

            // Measure consumption rate
            val consumptionRate = measureConsumptionRate(topicInfotrygd, kafkaSykepengedagerInformasjonConsumer)

            // Estimate time to consume remaining records (in milliseconds)
            val estimatedTimeMillis = totalUnconsumedRecords / consumptionRate * 1000

            // Convert milliseconds to a human-readable format
            val estimatedTimeSeconds = estimatedTimeMillis / 1000
            val estimatedTimeMinutes = estimatedTimeSeconds / 60
            val estimatedTimeHours = estimatedTimeMinutes / 60

            log.info(
                "Estimated time to consume remaining records from topic $topicInfotrygd: $estimatedTimeHours hours"
            )

            val allConsumed = totalRecordsToConsume == 0L

            if (allConsumed) {
                log.info("[MAX_DATE_RECORDS] All data from topic $topicInfotrygd is consumed.")
            } else {
                log.info("[MAX_DATE_RECORDS] There is still data to be consumed from topic $topicInfotrygd.")
            }
        }
    }

    private fun measureConsumptionRate(topic: String, kafkaConsumer: Consumer<String, String>): Double {
        log.info("[MAX_DATE_RECORDS] measureConsumptionRate $topic")
        val testRecords = 100L // Number of records to consume for testing
        val testPartition = kafkaConsumer.partitionsFor(topic).first()
        log.info("[MAX_DATE_RECORDS] measureConsumptionRate 1 $topic")

        val startTime = System.currentTimeMillis()

        kafkaConsumer.assign(listOf(TopicPartition(testPartition.topic(), testPartition.partition())))
        var consumedRecords = 0L

        log.info("[MAX_DATE_RECORDS] measureConsumptionRate 2 $topic")

        while (consumedRecords < testRecords) {
            log.info("[MAX_DATE_RECORDS] measureConsumptionRate  4 consumedRecords $consumedRecords $topic")

            val records = kafkaConsumer.poll(Duration.ofMillis(100))
            if (records.count() > 0) {
                log.info("[MAX_DATE_RECORDS] measureConsumptionRate 4 records count ${records.count()} $topic")
                consumedRecords += records.count()
            } else {
                break
            }
        }

        val endTime = System.currentTimeMillis()
        val consumptionTimeMillis = endTime - startTime
        log.info("[MAX_DATE_RECORDS] measureConsumptionRate 5 $topic")

        return consumedRecords.toDouble() / consumptionTimeMillis
    }
}
