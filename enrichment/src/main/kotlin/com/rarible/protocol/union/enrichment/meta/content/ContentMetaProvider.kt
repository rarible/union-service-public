package com.rarible.protocol.union.enrichment.meta.content

import com.rarible.core.content.meta.loader.ContentMetaReceiver
import com.rarible.core.meta.resource.model.ContentMeta
import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.content.cache.ContentCache
import com.rarible.protocol.union.enrichment.meta.content.cache.ContentCacheService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URL

@Component
class ContentMetaProvider(
    private val contentMetaService: ContentMetaService,
    private val contentCacheService: ContentCacheService,
    private val contentMetaReceiver: ContentMetaReceiver,
    private val metrics: ContentMetaMetrics,
    private val ff: FeatureFlagsProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getContent(itemId: ItemIdDto, resource: UrlResource): UnionMetaContentProperties? {
        val cache = getCache(resource)
        getFromCache(cache, itemId, resource)?.let { return it }

        val fetched = fetch(itemId, resource)
        updateCache(cache, itemId, resource, fetched)

        return fetched
    }

    private suspend fun getCache(resource: UrlResource): ContentCache? {
        return if (ff.enableContentMetaCache) {
            contentCacheService.getCache(resource)
        } else {
            null
        }
    }

    private suspend fun getFromCache(
        cache: ContentCache?,
        itemId: ItemIdDto,
        resource: UrlResource
    ): UnionMetaContentProperties? {
        val blockchain = itemId.blockchain
        if (cache == null) {
            metrics.onContentCacheSkipped(blockchain)
            return null
        }

        val cacheType = cache.getType()
        val fromCache = cache.get(resource)

        if (fromCache == null) {
            metrics.onContentCacheMiss(blockchain, cacheType)
            return null
        }

        metrics.onContentCacheHit(blockchain, cacheType)
        return fromCache.content
    }

    private suspend fun updateCache(
        cache: ContentCache?,
        itemId: ItemIdDto,
        resource: UrlResource,
        result: UnionMetaContentProperties?
    ) {
        cache ?: return

        val cacheType = cache.getType()
        val blockchain = itemId.blockchain

        when {
            result == null -> metrics.onContentCacheNotUpdated(blockchain, cacheType, "not_found")
            !result.isFull() -> metrics.onContentCacheNotUpdated(blockchain, cacheType, "not_full")
            else -> {
                cache.save(resource, result)
                metrics.onContentCacheUpdated(blockchain, cacheType)
            }
        }
    }

    private suspend fun fetch(itemId: ItemIdDto, resource: UrlResource): UnionMetaContentProperties? {
        val blockchain = itemId.blockchain
        val internalUrl = contentMetaService.resolveInternalHttpUrl(resource)
        if (internalUrl == resource.original) {
            logger.info("Fetching content meta by URL $internalUrl")
        } else {
            logger.info("Fetching content meta by URL $internalUrl (original URL is ${resource.original})")
        }

        val parsedUrl = try {
            URL(internalUrl)
        } catch (e: Throwable) {
            logger.warn("Wrong URL: $internalUrl", e)
            metrics.onContentResolutionFailed(itemId.blockchain, "remote", "malformed_url")
            return null
        }

        return try {
            val contentMeta = contentMetaReceiver.receive(parsedUrl)
            val properties = contentMeta?.let { contentMetaService.convertToProperties(contentMeta) }
            mark(blockchain, contentMeta, properties)
            properties
        } catch (e: Exception) {
            logger.warn("Failed to receive content meta via URL {}", internalUrl, e)
            metrics.onContentResolutionFailed(itemId.blockchain, "remote", "error")
            null
        }
    }

    private fun mark(
        blockchain: BlockchainDto,
        contentMeta: ContentMeta?,
        properties: UnionMetaContentProperties?
    ) {
        if (contentMeta == null) {
            metrics.onContentResolutionFailed(blockchain, "remote", "unresolvable")
            return
        }
        if (properties == null) {
            // if we got ContentMeta but after conversion it is null, it means we don't know such mime type
            metrics.onContentResolutionFailed(blockchain, "remote", "unknown_mime_type")
            return
        }
        metrics.onContentFetched(blockchain, "remote", properties)
    }

}