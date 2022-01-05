package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.cache.CacheService
import com.rarible.core.cache.get
import com.rarible.core.common.nowMillis
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
