package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.cache.CacheService
import com.rarible.core.cache.get
import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.configuration.MetaProperties
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@CaptureSpan(type = SpanType.APP)
class ContentMetaService(
    private val mediaMetaService: MediaMetaService,
    private val ipfsUrlResolver: IpfsUrlResolver,
    private val metaProperties: MetaProperties,
    @Autowired(required = false) private val cacheService: CacheService?
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun enrichContent(content: UnionMetaContent, itemId: ItemIdDto): UnionMetaContent {
        val properties = content.properties
        val enrichedProperties = if (properties == null || properties.isEmpty()) {
            val fetchedProperties = fetchMetaContentProperties(content.url, itemId)
            fetchedProperties ?: properties ?: UnionImageProperties()
        } else {
            properties
        }
        return content.copy(properties = enrichedProperties)
    }

    private suspend fun fetchMetaContentProperties(url: String, itemId: ItemIdDto): UnionMetaContentProperties? {
        val contentMeta = getContentMeta(url, itemId) ?: return null
        val isImage = contentMeta.type.contains("image")
        val isVideo = contentMeta.type.contains("video")
        val isAudio = contentMeta.type.contains("audio") // TODO: add dedicated properties for audio.
        return when {
            isImage -> UnionImageProperties(
                mimeType = contentMeta.type,
                width = contentMeta.width,
                height = contentMeta.height,
                size = contentMeta.size
            )
            isVideo || isAudio -> UnionVideoProperties(
                mimeType = contentMeta.type,
                width = contentMeta.width,
                height = contentMeta.height,
                size = contentMeta.size
            )
            else -> return null
        }
    }

    suspend fun getContentMeta(url: String, itemId: ItemIdDto? = null): ContentMeta? {
        val realUrl = ipfsUrlResolver.resolveRealUrl(url)
        val now = nowMillis()
        return try {
            if (metaProperties.returnOnlyCachedContentMeta) {
                logger.info("Returning only cached meta for $url")
                val onlyCachedDescriptor = object : CacheDescriptor<ContentMeta> {
                    override val collection: String
                        get() = mediaMetaService.collection

                    override fun get(id: String): Mono<ContentMeta> = Mono.empty()

                    override fun getMaxAge(value: ContentMeta?): Long = 0
                }
                cacheService?.getCached(realUrl, onlyCachedDescriptor)?.awaitFirstOrNull()
            } else {
                cacheService.get(realUrl, mediaMetaService, true).awaitFirstOrNull()
            }
        } catch (e: Throwable) {
            logger.warn("Failed to fetch meta for Item [{}] by URL {} ({}ms): {}", itemId, url, spent(now), e.message)
            null
        }
    }

    // TODO[meta-3.0]: accept [itemId] in addition to the raw URL and clear by it.
    suspend fun resetContentMeta(url: String) {
        val realUrl = ipfsUrlResolver.resolveRealUrl(url)
        runCatching {
            cacheService?.reset(realUrl, mediaMetaService)?.awaitFirstOrNull()
        }
    }
}
