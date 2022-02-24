package com.rarible.protocol.union.enrichment.service

import com.rarible.loader.cache.CacheLoaderService
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.UnionMetaMetrics
import com.rarible.protocol.union.enrichment.meta.getAvailable
import com.rarible.protocol.union.enrichment.meta.isMetaInitiallyLoadedOrFailed
import com.rarible.protocol.union.enrichment.meta.isMetaInitiallyScheduledForLoading
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class EnrichmentMetaService(
    @Qualifier("union.meta.cache.loader.service")
    private val unionMetaCacheLoaderService: CacheLoaderService<UnionMeta>,
    private val unionMetaMetrics: UnionMetaMetrics
) {
    private val logger = LoggerFactory.getLogger(EnrichmentMetaService::class.java)

    /**
     * Return available meta or `null` if it hasn't been loaded, has failed, or hasn't been requested yet.
     * Schedule an update in the last case.
     */
    suspend fun getAvailableMetaOrScheduleLoading(itemId: ItemIdDto): UnionMeta? {
        val metaCacheEntry = unionMetaCacheLoaderService.get(itemId.fullId())
        val availableMeta = metaCacheEntry.getAvailable()
        if (availableMeta != null) {
            return availableMeta
        }
        unionMetaMetrics.onMetaCacheMiss(itemId, null)
        if (metaCacheEntry.isMetaInitiallyLoadedOrFailed()) {
            return null
        }
        if (!metaCacheEntry.isMetaInitiallyScheduledForLoading()) {
            scheduleLoading(itemId)
        }
        return availableMeta
    }

    /**
     * Save pre-defined meta for an item. Useful in tests.
     */
    suspend fun save(itemId: ItemIdDto, unionMeta: UnionMeta) {
        unionMetaCacheLoaderService.save(itemId.fullId(), unionMeta)
    }

    /**
     * Schedule an update (or initial loading) of metadata.
     */
    suspend fun scheduleLoading(itemId: ItemIdDto) {
        logger.info("Scheduling meta update for {}", itemId.fullId())
        unionMetaCacheLoaderService.update(itemId.fullId())
    }
}
