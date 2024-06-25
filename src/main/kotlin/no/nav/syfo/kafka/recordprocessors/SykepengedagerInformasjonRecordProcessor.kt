package no.nav.syfo.kafka.recordprocessors

import org.apache.kafka.clients.consumer.ConsumerRecord

interface SykepengedagerInformasjonRecordProcessor {
    suspend fun processRecord(record: ConsumerRecord<String, String>)
}
