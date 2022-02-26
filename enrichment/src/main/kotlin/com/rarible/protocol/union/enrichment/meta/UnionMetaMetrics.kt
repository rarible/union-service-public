package com.rarible.protocol.union.enrichment.meta

import com.rarible.protocol.union.dto.ItemIdDto
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UnionMetaMetrics(
    private val meterRegistry: MeterRegistry
) {

    private val logger = LoggerFactory.getLogger(UnionMetaMetrics::class.java)

    private val metaCacheHits = Counter
        .builder(META_CACHE_HITS)
        .register(meterRegistry)

    private val metaCacheMisses = Counter
        .builder(META_CACHE_MISSES)
        .register(meterRegistry)

    fun onMetaCacheHitOrMiss(
        itemId: ItemIdDto,
        hitOrMiss: Boolean
    ) {
        if (hitOrMiss) {
            metaCacheHits.increment()
        } else {
            metaCacheMisses.increment()
        }
    }

    fun reset() {
        meterRegistry.clear()
    }

    private companion object {
        const val META_CACHE_HITS = "meta_cache_hits"
        const val META_CACHE_MISSES = "meta_cache_misses"
    }
}
