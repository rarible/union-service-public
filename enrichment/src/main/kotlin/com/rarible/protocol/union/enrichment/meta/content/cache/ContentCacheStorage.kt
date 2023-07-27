package com.rarible.protocol.union.enrichment.meta.content.cache

interface ContentCacheStorage {

    suspend fun get(url: String): UnionContentCacheEntry?

    suspend fun save(content: UnionContentCacheEntry)

    suspend fun delete(url: String)
}
