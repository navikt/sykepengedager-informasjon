@file:Suppress("TopLevelPropertyNaming")

package no.nav.syfo.config.kafka

import no.nav.syfo.kafka.producers.domain.KSykepengedagerInformasjonDTO
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties

const val topicAapSykepengedagerInfotrygd = "aap.sykepengedager.infotrygd.v1"
const val topicSykepengedagerInfotrygd = "team-esyfo.sykepengedager.infotrygd.v1"
const val topicUtbetaling = "tbd.utbetaling"

@Configuration
@EnableKafka
@Profile("remote")
class AivenKafkaConfig(
    @Value("\${kafka.brokers}") private val kafkaBrokers: String,
    @Value("\${kafka.truststore.path}") private val kafkaTruststorePath: String,
    @Value("\${kafka.keystore.path}") private val kafkaKeystorePath: String,
    @Value("\${kafka.credstore.password}") private val kafkaCredstorePassword: String,
) {

    fun commonConfig() = mapOf(
        CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers
    ) + securityConfig()

    private fun securityConfig() = mapOf(
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
        SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "", // Disable server host name verification
        SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "JKS",
        SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
        SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to kafkaTruststorePath,
        SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
        SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to kafkaKeystorePath,
        SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
        SslConfigs.SSL_KEY_PASSWORD_CONFIG to kafkaCredstorePassword
    )

    fun listenerContainerConfig() = mapOf(
        ConsumerConfig.GROUP_ID_CONFIG to "sykepengedager-informasjon-group-v1",
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
        ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
        ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "600000"
    ) + commonConfig()

    @Bean
    fun kafkaListenerContainerFactory(
        aivenKafkaErrorHandler: AivenKafkaErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val config = listenerContainerConfig()

        val consumerFactory = DefaultKafkaConsumerFactory<String, String>(config)

        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory
        factory.setCommonErrorHandler(aivenKafkaErrorHandler)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        return factory
    }

    @Bean
    fun kafkaSykepengedagerInformasjonConsumer(
        kafkaListenerContainerFactory: ConcurrentKafkaListenerContainerFactory<String, String>,
    ): Consumer<String, String> {
        val consumerFactory = kafkaListenerContainerFactory.consumerFactory
        val consumerProps = consumerFactory.configurationProperties

        return DefaultKafkaConsumerFactory(
            consumerProps,
            StringDeserializer(),
            StringDeserializer()
        ).createConsumer()
    }

    @Bean
    fun infotrygdConsumerFactory(): ConsumerFactory<String, String> {
        val config = listenerContainerConfig().toMutableMap()
        config.remove(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)
        config.remove(ConsumerConfig.GROUP_ID_CONFIG)
        config[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        config[ConsumerConfig.GROUP_ID_CONFIG] = "sykepengedager-informasjon-group-v2"

        return DefaultKafkaConsumerFactory(config as Map<String, Any>)
    }

    @Bean
    fun infotrygdKafkaListenerContainerFactory(
        aivenKafkaErrorHandler: AivenKafkaErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory =
            ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = infotrygdConsumerFactory()
        factory.setCommonErrorHandler(aivenKafkaErrorHandler)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        return factory
    }

    fun commonKafkaAivenProducerConfig(): HashMap<String, Any> {
        return HashMap<String, Any>().apply {
            putAll(commonConfig())
            put(
                ProducerConfig.ACKS_CONFIG,
                "all",
            )
            put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer::class.java,
            )
            put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JacksonKafkaSerializer::class.java,
            )
        }
    }

    @Bean
    fun sykepengedagerInformasjonProducerFactory(): ProducerFactory<String, KSykepengedagerInformasjonDTO> =
        DefaultKafkaProducerFactory(commonKafkaAivenProducerConfig())

    @Bean
    fun sykepengedagerInformasjonKafkaTemplate(producerFactory: ProducerFactory<String, KSykepengedagerInformasjonDTO>):
        KafkaTemplate<String, KSykepengedagerInformasjonDTO> = KafkaTemplate(producerFactory)
}
