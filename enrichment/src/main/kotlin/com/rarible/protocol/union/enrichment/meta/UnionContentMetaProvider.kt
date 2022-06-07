package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.content.meta.loader.ContentMetaReceiver
import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.enrichment.meta.cache.ContentCacheService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UnionContentMetaProvider(
    private val unionContentMetaService: UnionContentMetaService,
    private val contentCacheService: ContentCacheService,
    private val contentMetaReceiver: ContentMetaReceiver,
    private val ff: FeatureFlagsProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getContentMeta(resource: UrlResource): UnionMetaContentProperties? {
        val cache = if (ff.enableContentMetaCache) {
            contentCacheService.getCache(resource)
        } else {
            null
        }

        if (cache != null) {
            // TODO add metrics for hit/miss
            val fromCache = cache.get(resource)
            if (fromCache != null) {
                return fromCache.content
            }
        }

        val result = fetch(resource)

        if (cache != null) {
            // TODO add metrics for cases (not full, not found, saved)
            if (result != null && result.isFull()) {
                cache.save(resource, result)
            }
        }

        return result
    }

    private suspend fun fetch(resource: UrlResource): UnionMetaContentProperties? {
        val internalUrl = unionContentMetaService.resolveInternalHttpUrl(resource)
        if (internalUrl == resource.original) {
            logger.info("Fetching content meta by URL $internalUrl")
        } else {
            logger.info("Fetching content meta by URL $internalUrl (original URL is ${resource.original})")
        }

        return try {
            val contentMeta = contentMetaReceiver.receive(internalUrl)
            contentMeta?.let { unionContentMetaService.convertToProperties(it) }
        } catch (e: Exception) {
            logger.warn("Failed to receive content meta via URL {}", internalUrl, e)
            null
        }
    }

}