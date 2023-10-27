package com.rarible.protocol.union.enrichment.meta.content

import com.rarible.core.content.meta.loader.ContentMetaReceiver
import com.rarible.core.content.meta.loader.ContentMetaResult
import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.meta.content.cache.ContentCache
import com.rarible.protocol.union.enrichment.meta.content.cache.ContentCacheService
import com.rarible.protocol.union.enrichment.util.metaSpent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URL
import java.time.Instant

@Component
class ContentMetaProvider(
    private val contentMetaService: ContentMetaService,
    private val contentCacheService: ContentCacheService,
    private val contentMetaReceiver: ContentMetaReceiver,
    private val metrics: ContentMetaMetrics,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getContent(
        id: String,
        blockchain: BlockchainDto,
        start: Instant,
        resource: UrlResource
    ): UnionMetaContentProperties? {
        val cache = getCache(resource)
        getFromCache(cache, blockchain, resource)?.let { return it }

        val fetched = fetch(id, blockchain, start, resource)
        updateCache(cache, blockchain, resource, fetched)

        return fetched
    }

    private suspend fun getCache(resource: UrlResource): ContentCache? {
        return contentCacheService.getCache(resource)
    }

    private suspend fun getFromCache(
        cache: ContentCache?,
        blockchain: BlockchainDto,
        resource: UrlResource
    ): UnionMetaContentProperties? {
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
        blockchain: BlockchainDto,
        resource: UrlResource,
        result: UnionMetaContentProperties?
    ) {
        cache ?: return

        val cacheType = cache.getType()

        when {
            result == null -> metrics.onContentCacheNotUpdated(blockchain, cacheType, "not_found")
            !result.isFull() -> metrics.onContentCacheNotUpdated(blockchain, cacheType, "not_full")
            else -> {
                cache.save(resource, result)
                metrics.onContentCacheUpdated(blockchain, cacheType)
            }
        }
    }

    private suspend fun fetch(
        id: String,
        blockchain: BlockchainDto,
        start: Instant,
        resource: UrlResource
    ): UnionMetaContentProperties? {
        val internalUrl = contentMetaService.resolveInternalHttpUrl(resource)

        val parsedUrl = try {
            URL(internalUrl)
        } catch (e: Throwable) {
            logger.warn("Wrong URL for $id: $internalUrl: ${e.message}")
            metrics.onContentResolutionFailed(
                blockchain = blockchain,
                start = start,
                source = SOURCE,
                approach = "unknown",
                reason = "malformed_url",
            )
            return null
        }

        return try {
            val result = contentMetaReceiver.receive(parsedUrl)
            val properties = result.meta?.let { contentMetaService.convertToProperties(it) }
            mark(start, blockchain, result, properties)

            val spent = System.currentTimeMillis() - start.toEpochMilli()
            if (result.exception == null) {
                logger.info(
                    "Fetched content meta for $id via URL $internalUrl (original URL: ${resource.original})," +
                        " resolved with approach: ${result.approach}, bytes read: ${result.bytesRead}," +
                        " content=${result.meta} (${metaSpent(start)})"
                )
            } else {
                logger.warn(
                    "Failed to fetch content meta for $id via URL $internalUrl (original URL: ${resource.original})," +
                        " resolved with approach: ${result.approach}, bytes read: ${result.bytesRead}," +
                        " error=${result.exception?.message} (${metaSpent(start)})"
                )
            }

            properties
        } catch (e: Exception) {
            logger.warn("Failed to receive content meta for $id via URL $internalUrl", e)
            metrics.onContentResolutionFailed(
                blockchain = blockchain,
                start = start,
                source = SOURCE,
                approach = "unknown",
                reason = "error",
            )
            null
        }
    }

    private fun mark(
        start: Instant,
        blockchain: BlockchainDto,
        result: ContentMetaResult,
        properties: UnionMetaContentProperties?
    ) {
        if (result.meta == null) {
            metrics.onContentResolutionFailed(
                blockchain = blockchain,
                start = start,
                source = SOURCE,
                approach = result.approach,
                reason = "unresolvable",
            )
            return
        }
        if (properties == null) {
            // if we got ContentMeta but after conversion it is null, it means we don't know such mime type
            metrics.onContentResolutionFailed(
                blockchain = blockchain,
                start = start,
                source = SOURCE,
                approach = result.approach,
                reason = "unknown_mime_type",
            )
            return
        }
        metrics.onContentFetched(
            blockchain = blockchain,
            start = start,
            source = SOURCE,
            approach = result.approach,
            properties = properties,
        )
    }

    companion object {
        const val SOURCE = "remote"
    }
}
