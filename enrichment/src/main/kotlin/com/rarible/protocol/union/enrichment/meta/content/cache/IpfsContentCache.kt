package com.rarible.protocol.union.enrichment.meta.content.cache

import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.model.UrlResource
import org.springframework.stereotype.Component

@Component
class IpfsContentCache(
    storage: ContentCacheStorage
) : AbstractContentCache(storage) {

    override fun getType() = "ipfs"

    override fun isSupported(urlResource: UrlResource): Boolean {
        return urlResource is IpfsUrl
    }

    override fun getUrlKey(urlResource: UrlResource): String {
        if (!isSupported(urlResource)) {
            throw ContentCacheException("${javaClass.simpleName} doesn't support URL resource $urlResource")
        }
        return (urlResource as IpfsUrl).toSchemaUrl()
    }
}
