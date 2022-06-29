package com.rarible.protocol.union.enrichment.meta.content.cache

import com.rarible.core.meta.resource.model.UrlResource
import org.springframework.stereotype.Component

@Component
class ContentCacheService(
    private val caches: List<ContentCache>
) {

    fun getCache(urlResource: UrlResource): ContentCache? {
        return caches.find { it.isSupported(urlResource) }
    }

}