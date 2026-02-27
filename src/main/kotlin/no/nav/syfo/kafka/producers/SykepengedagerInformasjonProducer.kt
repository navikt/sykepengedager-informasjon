package no.nav.syfo.kafka.producers

import no.nav.syfo.db.SendingFailedSykepengedagerInformasjonDAO
import no.nav.syfo.db.SentSykepengedagerInformasjonDAO
import no.nav.syfo.kafka.producers.domain.KSykepengedagerInformasjonDTO
import no.nav.syfo.logger
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.KafkaException
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ExecutionException

@Component
class SykepengedagerInformasjonProducer(
    private val kafkaTemplate: KafkaTemplate<String, KSykepengedagerInformasjonDTO>,
    private val sentSykepengedagerInformasjonDAO: SentSykepengedagerInformasjonDAO,
    private val sendingFailedSykepengedagerInformasjonDAO: SendingFailedSykepengedagerInformasjonDAO,
) {
    fun publishSykepengedagerInformasjon(event: KSykepengedagerInformasjonDTO,) {
        try {
            log.info(
                "SykepengedagerInformasjonProducer: Publishing sykepengedager-informasjon-topic with id: ${event.id}"
            )
            kafkaTemplate.send(
                ProducerRecord(
                    SYKEPENGEDAGER_INFORMASJON_TOPIC,
                    UUID.randomUUID().toString(),
                    event,
                ),
            ).get()
            sentSykepengedagerInformasjonDAO.storeSentSykepengedagerInformasjon(event, event.createdAt)
            log.info("Successfully sent event to sykepengedager-informasjon-topic")
        } catch (e: ExecutionException) {
            log.error(
                "ExecutionException was thrown when attempting to " +
                    "publish response to ${SYKEPENGEDAGER_INFORMASJON_TOPIC}. ${e.message}",
            )
            sendingFailedSykepengedagerInformasjonDAO.storeSendingFailedSykepengedagerInformasjon(
                event,
                e.message ?: "",
                event.createdAt,
            )
            throw e
        } catch (e: KafkaException) {
            log.error(
                "KafkaException was thrown when attempting to " +
                    "publish response to ${SYKEPENGEDAGER_INFORMASJON_TOPIC}. ${e.message}",
            )
            sendingFailedSykepengedagerInformasjonDAO.storeSendingFailedSykepengedagerInformasjon(
                event,
                e.message ?: "",
                event.createdAt,
            )
            throw e
        }
    }

    companion object {
        const val SYKEPENGEDAGER_INFORMASJON_TOPIC = "team-esyfo.sykepengedager-informasjon-topic"
        private val log = logger()
    }
}
