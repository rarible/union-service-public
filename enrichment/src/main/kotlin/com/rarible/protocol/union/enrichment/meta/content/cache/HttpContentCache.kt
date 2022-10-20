package com.rarible.protocol.union.enrichment.meta.content.cache

import com.rarible.core.meta.resource.model.HttpUrl
import com.rarible.core.meta.resource.model.UrlResource
import org.springframework.stereotype.Component

@Component
class HttpContentCache(
    storage: ContentCacheStorage
) : AbstractContentCache(storage) {

    override fun getType(): String = "http"

    override fun isSupported(urlResource: UrlResource): Boolean = urlResource is HttpUrl

    override fun getUrlKey(urlResource: UrlResource): String = urlResource.original
}