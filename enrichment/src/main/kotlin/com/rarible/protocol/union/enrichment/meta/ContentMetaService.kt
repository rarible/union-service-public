package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.cache.CacheService
import com.rarible.core.cache.get
import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ContentMetaService(
    private val mediaMetaService: MediaMetaService,
    private val ipfsUrlResolver: IpfsUrlResolver,
    @Autowired(required = false) private val cacheService: CacheService?
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getContentMeta(url: String, itemId: String? = null): ContentMeta? {
        val realUrl = ipfsUrlResolver.resolveRealUrl(url)
        val now = nowMillis()
        try {
            return cacheService.get(realUrl, mediaMetaService, true).awaitFirstOrNull()
        } catch (e: Throwable) {
            logger.warn("Failed to fetch meta for Item [{}] by URL {} ({}ms): {}", itemId, url, spent(now), e.message)
            return null
        }
    }

    suspend fun resetContentMeta(url: String) {
        val realUrl = ipfsUrlResolver.resolveRealUrl(url)
        runCatching {
            cacheService?.reset(realUrl, mediaMetaService)?.awaitFirstOrNull()
        }
    }
}
