package com.rarible.protocol.union.enrichment.service

import com.rarible.core.loader.LoadTaskStatus
import com.rarible.loader.cache.CacheEntry
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.UnionMetaMetrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.time.withTimeoutOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class EnrichmentMetaService(
    @Qualifier("union.meta.cache.loader.service")
    private val unionMetaCacheLoaderService: CacheLoaderService<UnionMeta>,
    private val unionMetaMetrics: UnionMetaMetrics
) {
    private val logger = LoggerFactory.getLogger(EnrichmentMetaService::class.java)

    /**
     * Get available item metadata. Return `null` if the meta hasn't been requested yet,
     * has failed to be loaded or is being loaded now.
     */
    suspend fun getAvailableMeta(itemId: ItemIdDto): UnionMeta? {
        val meta = unionMetaCacheLoaderService.getAvailable(itemId.fullId())
        if (meta == null) {
            unionMetaMetrics.onMetaCacheMiss(itemId, null)
        }
        return meta
    }

    /**
     * Return available meta (same as [getAvailableMeta]) but additionally schedule an update
     * if the meta hasn't been requested yet.
     */
    suspend fun getAvailableMetaOrScheduleLoading(itemId: ItemIdDto): UnionMeta? {
        val availableMeta = getAvailableMeta(itemId)
        if (availableMeta == null && !isMetaInitiallyScheduledForLoading(itemId)) {
            scheduleLoading(itemId)
        }
        return availableMeta
    }

    /**
     * Return available meta (same as [getAvailableMeta]) if the meta has been loaded
     * or its loading has failed (`null` in this case), or schedule a meta update
     * and wait up to [loadingWaitTimeout] until the meta is loaded or failed.
     */
    suspend fun getAvailableMetaOrScheduleAndWait(
        itemId: ItemIdDto,
        loadingWaitTimeout: Duration
    ): UnionMeta? {
        val availableMeta = getAvailableMetaOrScheduleLoading(itemId)
        if (availableMeta != null) {
            return availableMeta
        }
        if (isMetaInitiallyLoadedOrFailed(itemId)) {
            return getAvailableMeta(itemId)
        }
        logger.info(
            "Starting to wait for the meta loading of {} for {} ms",
            itemId.fullId(),
            loadingWaitTimeout.toMillis()
        )
        return withTimeoutOrNull(loadingWaitTimeout) {
            while (isActive) {
                if (isMetaInitiallyLoadedOrFailed(itemId)) {
                    // Return meta as is, irrespective whether an error has happened or the meta has been loaded.
                    val meta = getAvailableMeta(itemId)
                    if (meta == null) {
                        unionMetaMetrics.onMetaCacheMiss(itemId, loadingWaitTimeout)
                    }
                    return@withTimeoutOrNull meta
                }
                delay(100)
            }
            return@withTimeoutOrNull null
        }
    }

    /**
     * Schedule an update (or initial loading) of metadata.
     */
    suspend fun scheduleLoading(itemId: ItemIdDto) {
        logger.info("Scheduling meta update for {}", itemId.fullId())
        unionMetaCacheLoaderService.update(itemId.fullId())
    }

    /**
     * Returns true if loading of the meta for an item has been scheduled in the past,
     * no matter what the loading result is (in progress, failed or success).
     */
    private suspend fun isMetaInitiallyScheduledForLoading(itemId: ItemIdDto): Boolean =
        when (unionMetaCacheLoaderService.get(itemId.fullId())) {
            // Let's use full 'when' expression to not forget some if branches.
            is CacheEntry.NotAvailable -> false
            is CacheEntry.Loaded,
            is CacheEntry.LoadedAndUpdateScheduled,
            is CacheEntry.LoadedAndUpdateFailed,
            is CacheEntry.InitialLoadScheduled,
            is CacheEntry.InitialFailed -> true
        }

    /**
     * Returns `true` if the meta for item has been loaded or loading has failed,
     * and `false` if we haven't requested the meta loading or haven't received any result yet.
     */
    private suspend fun isMetaInitiallyLoadedOrFailed(itemId: ItemIdDto): Boolean =
        when (val cacheEntry = unionMetaCacheLoaderService.get(itemId.fullId())) {
            is CacheEntry.Loaded -> true
            is CacheEntry.LoadedAndUpdateScheduled -> true
            is CacheEntry.LoadedAndUpdateFailed -> true
            is CacheEntry.InitialLoadScheduled -> when (cacheEntry.loadStatus) {
                is LoadTaskStatus.Scheduled -> false
                is LoadTaskStatus.WaitsForRetry -> true
            }
            is CacheEntry.InitialFailed -> true
            is CacheEntry.NotAvailable -> false
        }

}
