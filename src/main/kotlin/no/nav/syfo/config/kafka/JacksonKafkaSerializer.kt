package no.nav.syfo.config.kafka

import org.apache.kafka.common.serialization.Serializer
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

fun jacksonMapper(): JsonMapper = JsonMapper
    .builder()
    .addModule(KotlinModule.Builder().build())
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
    .build()

class JacksonKafkaSerializer : Serializer<Any> {
    override fun serialize(topic: String?, data: Any?,): ByteArray = jacksonMapper().writeValueAsBytes(data)

    @Suppress("EmptyFunctionBlock")
    override fun close() {
    }
}
