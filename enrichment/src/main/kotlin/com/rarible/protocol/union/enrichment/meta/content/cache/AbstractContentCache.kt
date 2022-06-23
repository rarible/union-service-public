package com.rarible.protocol.union.enrichment.meta.content.cache

import com.rarible.core.common.nowMillis
import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.protocol.union.core.model.UnionMetaContentProperties

abstract class AbstractContentCache(
    private val storage: ContentCacheStorage
) : ContentCache {

    abstract fun getUrlKey(urlResource: UrlResource): String

    override suspend fun get(urlResource: UrlResource): UnionContentCacheEntry? {
        val urlKey = getUrlKey(urlResource)
        return storage.get(urlKey)
    }

    override suspend fun save(urlResource: UrlResource, content: UnionMetaContentProperties): UnionContentCacheEntry {
        if (!content.isFull()) {
            throw ContentCacheException("Can't save $content to cache - data is not full")
        }

        val entry = UnionContentCacheEntry(
            url = getUrlKey(urlResource),
            type = getType(),
            updatedAt = nowMillis(),
            content = content
        )

        storage.save(entry)
        return entry
    }

}