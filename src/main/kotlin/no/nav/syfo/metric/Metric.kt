package no.nav.syfo.metric

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class Metric
@Autowired
constructor(
    private val meterRegistry: MeterRegistry,
) {
    fun createTimer(
        queryName: String,
        builderName: String,
    ): Timer =
        Timer
            .builder(builderName)
            .tag("query", queryName)
            .register(meterRegistry)

    fun countSomeEventComplete() = countEvent("event_name_todo")

    fun countEvent(name: String) {
        meterRegistry
            .counter(
                metricPrefix(name),
                Tags.of("type", "info"),
            ).increment()
    }

    private fun metricPrefix(name: String) = "sykepengedager-informasjon_$name"
}

enum class TimerBuilderName {
    DATABASE_QUERY_LATENCY,
    REST_CALL_LATENCY,
}
