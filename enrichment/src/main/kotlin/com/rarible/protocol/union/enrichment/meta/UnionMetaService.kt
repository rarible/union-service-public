package com.rarible.protocol.union.enrichment.meta

import com.rarible.loader.cache.CacheLoaderService
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import kotlinx.coroutines.time.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class UnionMetaService(
    @Qualifier("union.meta.cache.loader.service")
    private val unionMetaCacheLoaderService: CacheLoaderService<UnionMeta>,
    private val unionMetaMetrics: UnionMetaMetrics,
    private val unionMetaCacheLoader: UnionMetaCacheLoader
) {
    private val logger = LoggerFactory.getLogger(UnionMetaService::class.java)

    /**
     * Return available meta or `null` if it hasn't been loaded, has failed, or hasn't been requested yet.
     * Schedule an update in the last case.
     */
    suspend fun getAvailableMetaOrScheduleLoading(itemId: ItemIdDto): UnionMeta? =
        getAvailableMetaOrLoadSynchronously(itemId, false)

    /**
     * Return available meta, if any. Otherwise, load the meta in the current coroutine (it may be slow).
     * Additionally, schedule loading if the meta hasn't been requested for this item.
     */
    suspend fun getAvailableMetaOrLoadSynchronously(
        itemId: ItemIdDto,
        synchronous: Boolean
    ): UnionMeta? {
        val metaCacheEntry = unionMetaCacheLoaderService.get(itemId.fullId())
        val availableMeta = metaCacheEntry.getAvailable()
        unionMetaMetrics.onMetaCacheHitOrMiss(
            itemId = itemId,
            hitOrMiss = availableMeta != null
        )
        if (availableMeta != null) {
            return availableMeta
        }
        if (metaCacheEntry.isMetaInitiallyLoadedOrFailed()) {
            return null
        }
        if (!metaCacheEntry.isMetaInitiallyScheduledForLoading()) {
            scheduleLoading(itemId)
        }
        if (synchronous) {
            return unionMetaCacheLoader.load(itemId.fullId())
        }
        return null
    }

    /**
     * The same as [getAvailableMetaOrLoadSynchronously] but with [timeout].
     */
    suspend fun getAvailableMetaOrLoadSynchronouslyWithTimeout(
        itemId: ItemIdDto,
        timeout: Duration
    ): UnionMeta? = try {
        withTimeout(timeout) {
            getAvailableMetaOrLoadSynchronously(
                itemId = itemId,
                synchronous = true
            )
        }
    } catch (e: Exception) {
        logger.error("Cannot synchronously load meta for ${itemId.fullId()} with timeout ${timeout.toMillis()} ms", e)
        null
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
