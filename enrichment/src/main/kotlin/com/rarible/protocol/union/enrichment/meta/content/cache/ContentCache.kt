package com.rarible.protocol.union.enrichment.meta.content.cache

import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.protocol.union.core.model.UnionMetaContentProperties

interface ContentCache {

    fun getType(): String

    fun isSupported(urlResource: UrlResource): Boolean

    suspend fun get(urlResource: UrlResource): UnionContentCacheEntry?

    suspend fun save(urlResource: UrlResource, content: UnionMetaContentProperties): UnionContentCacheEntry
}