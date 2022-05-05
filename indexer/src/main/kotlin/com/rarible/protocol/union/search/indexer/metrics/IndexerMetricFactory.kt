package com.rarible.protocol.union.search.indexer.metrics

import com.rarible.core.telemetry.metrics.LongGaugeMetric
import com.rarible.core.telemetry.metrics.RegisteredGauge
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.search.indexer.config.IndexerProperties
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class IndexerMetricFactory(
    private val meterRegistry: MeterRegistry,
    private val properties: IndexerProperties,
) {

    fun createEventHandlerGaugeMetric(entity: EsEntity): RegisteredGauge<Long> {
        return object : LongGaugeMetric(
            name = eventHandlerGaugeMetric(entity)
        ){}.bind(meterRegistry)
    }

    private fun eventHandlerGaugeMetric(entity: EsEntity): String {
        return "${properties.metrics.rootPath}.event.${entity.entityName}"
    }
}
