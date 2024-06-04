package no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.spleis

import no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.spleis.domain.UTBETALING_UTBETALT
import no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.spleis.domain.UTBETALING_UTEN_UTBETALING
import no.nav.syfo.sykepengedagerinformasjon.kafka.consumers.spleis.domain.UtbetalingSpleis
import no.nav.syfo.sykepengedagerinformasjon.config.kafka.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer

@EnableKafka
@Configuration
class SpleisKafkaConfig(
    private val kafkaConfig: KafkaConfig,
) {
    @Bean
    fun spleisConsumerFactory(): ConsumerFactory<String, UtbetalingSpleis> {
        return DefaultKafkaConsumerFactory(
            kafkaConfig.commonKafkaAivenConfig().apply { kafkaConfig.commonKafkaAivenConsumerConfig() },
            StringDeserializer(),
            JsonDeserializer(UtbetalingSpleis::class.java)
        )
    }

    @Bean
    fun spleisKafkaListenerWithFilterContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, UtbetalingSpleis> {
        val factory: ConcurrentKafkaListenerContainerFactory<String, UtbetalingSpleis> =
            ConcurrentKafkaListenerContainerFactory<String, UtbetalingSpleis>()
        factory.consumerFactory = spleisConsumerFactory()
        factory.setRecordFilterStrategy { record: ConsumerRecord<String?, UtbetalingSpleis> ->
            record.value().event.contains(UTBETALING_UTBETALT) || record.value().event.contains(
                UTBETALING_UTEN_UTBETALING
            )
        }
        return factory
    }
}
