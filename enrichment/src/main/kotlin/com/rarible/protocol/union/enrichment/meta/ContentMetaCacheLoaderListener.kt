package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.content.meta.loader.ContentMeta
import com.rarible.loader.cache.CacheEntry
import com.rarible.loader.cache.CacheLoaderEvent
import com.rarible.loader.cache.CacheLoaderEventListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ContentMetaCacheLoaderListener : CacheLoaderEventListener<ContentMeta> {

    private val logger = LoggerFactory.getLogger(ContentMetaCacheLoaderListener::class.java)

    override val type
        get() = ContentMetaCacheLoader.TYPE

    // Currently, just log success/failures of loading content meta.
    override suspend fun onEvent(cacheLoaderEvent: CacheLoaderEvent<ContentMeta>) {
        val key = cacheLoaderEvent.key
        return when (val cacheEntry = cacheLoaderEvent.cacheEntry) {
            is CacheEntry.Loaded -> logger.info("Loaded content meta for $key")
            is CacheEntry.LoadedAndUpdateFailed -> logger.info("Failed to update content meta for $key: ${cacheEntry.failedUpdateStatus.errorMessage}")
            is CacheEntry.InitialFailed -> logger.info("Failed to load content meta for $key: ${cacheEntry.failedStatus.errorMessage}")
            is CacheEntry.LoadedAndUpdateScheduled -> return
            is CacheEntry.InitialLoadScheduled -> return
            is CacheEntry.NotAvailable -> return
        }
    }
}
