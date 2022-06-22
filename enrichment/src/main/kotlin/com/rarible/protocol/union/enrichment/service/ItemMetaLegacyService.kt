package com.rarible.protocol.union.enrichment.service

import com.rarible.core.apm.SpanType
import com.rarible.core.apm.withSpan
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.getAvailable
import com.rarible.protocol.union.enrichment.meta.isMetaInitiallyLoadedOrFailed
import com.rarible.protocol.union.enrichment.meta.isMetaInitiallyScheduledForLoading
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaLoader
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaMetrics
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component

@Component
@Deprecated("Should be replaced in epic Meta 3.0: Pipeline")
class ItemMetaLegacyService(
    @Qualifier("union.meta.cache.loader.service")
    private val unionMetaCacheLoaderService: CacheLoaderService<UnionMeta>,
    private val metrics: ItemMetaMetrics,
    private val itemMetaLoader: ItemMetaLoader
) : ItemMetaService {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Return available meta or `null` if it hasn't been loaded, has failed, or hasn't been requested yet.
     * For missed meta no scheduling operations will be performed
     */
    override suspend fun get(itemIds: List<ItemIdDto>, pipeline: String): Map<ItemIdDto, UnionMeta> {
        val keyMap = itemIds.associateBy { it.fullId() }
        val result = HashMap<ItemIdDto, UnionMeta>()
        val cached = withSpan(name = "fetchCachedMeta", type = SpanType.CACHE) {
            unionMetaCacheLoaderService.getAll(keyMap.keys.toList())
        }
        cached.forEach {
            val id = keyMap[it.key]!!
            if (it.isMetaInitiallyLoadedOrFailed()) {
                metrics.onMetaCacheMiss(id.blockchain)
            } else {
                metrics.onMetaCacheHit(id.blockchain)
            }
            val meta = it.getAvailable()
            meta?.let { result[id] = meta }
        }
        return result
    }

    /**
     * Return available meta, if any. Otherwise, load the meta in the current coroutine (it may be slow).
     * Additionally, schedule loading if the meta hasn't been requested for this item.
     */
    override suspend fun get(
        itemId: ItemIdDto,
        sync: Boolean,
        pipeline: String
    ): UnionMeta? {
        val metaCacheEntry = unionMetaCacheLoaderService.get(itemId.fullId())
        val availableMeta = metaCacheEntry.getAvailable()

        if (metaCacheEntry.isMetaInitiallyLoadedOrFailed()) {
            metrics.onMetaCacheMiss(itemId.blockchain)
        } else {
            metrics.onMetaCacheHit(itemId.blockchain)
        }

        if (availableMeta != null) {
            return availableMeta
        }

        if (metaCacheEntry.isMetaInitiallyLoadedOrFailed()) {
            logger.info("Meta loading for item ${itemId.fullId()} was failed")
            return null
        }

        if (!sync && !metaCacheEntry.isMetaInitiallyScheduledForLoading()) {
            schedule(itemId, pipeline, false)
        }
        if (sync) {
            logger.info("Loading meta synchronously for ${itemId.fullId()}")
            val result = download(itemId, pipeline, false)
            if (result == null && !metaCacheEntry.isMetaInitiallyScheduledForLoading()) {
                schedule(itemId, pipeline, false)
            }
            return result
        }
        return null
    }

    override suspend fun download(
        itemId: ItemIdDto,
        pipeline: String,
        force: Boolean
    ): UnionMeta? {
        val itemMeta = try {
            itemMetaLoader.load(itemId)
        } catch (e: Exception) {
            logger.warn("Synchronous meta loading failed for ${itemId.fullId()}")
            null
        }
        if (itemMeta != null) {
            logger.info("Saving synchronously loaded meta to cache for ${itemId.fullId()}")
            try {
                unionMetaCacheLoaderService.save(itemId.fullId(), itemMeta)
            } catch (e: Exception) {
                if (e !is OptimisticLockingFailureException && e !is DuplicateKeyException) {
                    logger.error("Failed to save synchronously loaded meta to cache for ${itemId.fullId()}")
                    throw e
                }
            }
        }
        return itemMeta
    }

    /**
     * Save pre-defined meta for an item. Useful in tests.
     */
    override suspend fun save(itemId: ItemIdDto, meta: UnionMeta) {
        unionMetaCacheLoaderService.save(itemId.fullId(), meta)
    }

    /**
     * Schedule an update (or initial loading) of metadata.
     */
    override suspend fun schedule(
        itemId: ItemIdDto,
        pipeline: String,
        force: Boolean
    ) {
        logger.info("Scheduling meta update for {}", itemId.fullId())
        unionMetaCacheLoaderService.update(itemId.fullId())
    }
}
