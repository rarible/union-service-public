package com.rarible.protocol.union.enrichment.meta.collection

import com.rarible.protocol.union.enrichment.meta.MetaMetrics
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class CollectionMetaMetrics(
    meterRegistry: MeterRegistry
) : MetaMetrics(meterRegistry, "collection")
