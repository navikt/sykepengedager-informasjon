@file:Suppress("TopLevelPropertyNaming")

package no.nav.syfo.sykepengedagerinformasjon.config.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

const val topicSykepengedagerInfotrygd = "aap.sykepengedager.infotrygd.v1"
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

    @Bean
    fun kafkaListenerContainerFactory(
        aivenKafkaErrorHandler: AivenKafkaErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val config = mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to "sykepengedager-informasjon-group-v1",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "600000"
        ) + commonConfig()
        val consumerFactory = DefaultKafkaConsumerFactory<String, String>(config)

        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory
        factory.setCommonErrorHandler(aivenKafkaErrorHandler)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        return factory
    }
}
