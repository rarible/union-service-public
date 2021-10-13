package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.cache.CacheService
import com.rarible.core.cache.get
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ContentMetaService(
    private val mediaMetaService: MediaMetaService,
    private val ipfsService: IpfsService,
    @Autowired(required = false) private val cacheService: CacheService?
) {
    suspend fun getContentMeta(url: String): ContentMeta? {
        val realUrl = ipfsService.resolveRealUrl(url)
        return runCatching {
            cacheService.get(realUrl, mediaMetaService, true).awaitFirstOrNull()
        }.getOrNull()
    }

    suspend fun resetContentMeta(url: String) {
        val realUrl = ipfsService.resolveRealUrl(url)
        runCatching {
            cacheService?.reset(realUrl, mediaMetaService)?.awaitFirstOrNull()
        }
    }
}
