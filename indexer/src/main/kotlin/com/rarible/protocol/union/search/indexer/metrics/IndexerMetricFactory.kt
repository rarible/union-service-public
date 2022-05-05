package com.rarible.protocol.union.search.indexer.metrics

import com.rarible.core.telemetry.metrics.LongGaugeMetric
import com.rarible.core.telemetry.metrics.RegisteredGauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class IndexerMetricFactory(
    private val meterRegistry: MeterRegistry,
) {
    companion object {
        const val rootPath: String = "protocol.union.indexer"
    }

    fun createEventHandlerGaugeMetric(entity: String): RegisteredGauge<Long> {
        return object : LongGaugeMetric(
            name = eventHandlerGaugeMetric(entity)
        ){}.bind(meterRegistry)
    }

    private fun eventHandlerGaugeMetric(entity: String): String {
        return "${rootPath}.event-handler.${entity}"
    }
}
