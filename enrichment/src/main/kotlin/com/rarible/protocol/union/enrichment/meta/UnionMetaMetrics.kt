package com.rarible.protocol.union.enrichment.meta

import com.rarible.protocol.union.dto.ItemIdDto
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class UnionMetaMetrics(
    private val meterRegistry: MeterRegistry
) {

    private val logger = LoggerFactory.getLogger(UnionMetaMetrics::class.java)

    private val metaCacheMisses = Counter
        .builder(META_CACHE_MISSES)
        .register(meterRegistry)

    private val metaCacheMissesWithSyncLoading = Counter
        .builder(META_CACHE_MISSES)
        .tag("sync", "true")
        .register(meterRegistry)

    fun onMetaCacheMiss(
        itemId: ItemIdDto,
        loadingWaitTimeout: Duration?
    ) {
        metaCacheMisses.increment()
        logger.info(
            buildString {
                append("Meta for item $itemId is not available")
                if (loadingWaitTimeout != null) {
                    append(" even though we waited for loading with timeout of ${loadingWaitTimeout.toMillis()} ms")
                }
            }
        )
        if (loadingWaitTimeout != null) {
            metaCacheMissesWithSyncLoading.increment()
        }
    }

    fun reset() {
        meterRegistry.clear()
    }

    private companion object {
        const val META_CACHE_MISSES = "meta_cache_misses"
    }
}
