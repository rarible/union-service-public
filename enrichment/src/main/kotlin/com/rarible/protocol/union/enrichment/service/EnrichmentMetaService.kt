package com.rarible.protocol.union.enrichment.service

import com.rarible.loader.cache.CacheLoaderService
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.UnionMetaLoadingAwaitService
import com.rarible.protocol.union.enrichment.meta.UnionMetaMetrics
import com.rarible.protocol.union.enrichment.meta.getAvailable
import com.rarible.protocol.union.enrichment.meta.isMetaInitiallyLoadedOrFailed
import com.rarible.protocol.union.enrichment.meta.isMetaInitiallyScheduledForLoading
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class EnrichmentMetaService(
    @Qualifier("union.meta.cache.loader.service")
    private val unionMetaCacheLoaderService: CacheLoaderService<UnionMeta>,
    private val unionMetaLoadingAwaitService: UnionMetaLoadingAwaitService,
    private val unionMetaMetrics: UnionMetaMetrics
) {
    private val logger = LoggerFactory.getLogger(EnrichmentMetaService::class.java)

    /**
     * Return available meta or `null` if it hasn't been loaded, has failed, or hasn't been requested yet.
     * Schedule an update in the last case.
     */
    suspend fun getAvailableMetaOrScheduleLoading(itemId: ItemIdDto): UnionMeta? =
        getAvailableMetaOrScheduleLoadingAndWaitWithTimeout(itemId, null)

    /**
     * Same as [getAvailableMetaOrScheduleLoading] and synchronously (in a coroutine) wait up to
     * [loadingWaitTimeout] until the meta is loaded or failed.
     */
    suspend fun getAvailableMetaOrScheduleLoadingAndWaitWithTimeout(
        itemId: ItemIdDto,
        loadingWaitTimeout: Duration?
    ): UnionMeta? {
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
        if (loadingWaitTimeout == null) {
            return null
        }
        return unionMetaLoadingAwaitService.waitForMetaLoadingWithTimeout(itemId, loadingWaitTimeout)
    }

    /**
     * Schedule an update (or initial loading) of metadata.
     */
    suspend fun scheduleLoading(itemId: ItemIdDto) {
        logger.info("Scheduling meta update for {}", itemId.fullId())
        unionMetaCacheLoaderService.update(itemId.fullId())
    }
}
