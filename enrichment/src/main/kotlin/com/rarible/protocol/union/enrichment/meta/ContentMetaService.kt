package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.cache.CacheService
import com.rarible.core.cache.get
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ContentMetaService(
    private val mediaMetaService: MediaMetaService,
    private val ipfsUrlResolver: IpfsUrlResolver,
    @Autowired(required = false) private val cacheService: CacheService?
) {

    suspend fun getContentMeta(url: String): ContentMeta? {
        val realUrl = ipfsUrlResolver.resolveRealUrl(url)
        return runCatching {
            cacheService.get(realUrl, mediaMetaService, true).awaitFirstOrNull()
        }.getOrNull()
    }

    // TODO UNION We should use this method
    suspend fun resetContentMeta(url: String) {
        val realUrl = ipfsUrlResolver.resolveRealUrl(url)
        runCatching {
            cacheService?.reset(realUrl, mediaMetaService)?.awaitFirstOrNull()
        }
    }
}
