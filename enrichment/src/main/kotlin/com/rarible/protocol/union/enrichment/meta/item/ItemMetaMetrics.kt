package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.enrichment.meta.MetaMetrics
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class ItemMetaMetrics(
    meterRegistry: MeterRegistry
) : MetaMetrics(
    meterRegistry,
    "item",
    ItemMetaPipeline.values().map { it.pipeline }
)
