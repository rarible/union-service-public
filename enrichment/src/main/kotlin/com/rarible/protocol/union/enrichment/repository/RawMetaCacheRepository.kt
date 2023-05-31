package com.rarible.protocol.union.enrichment.repository

import com.rarible.protocol.union.enrichment.model.RawMetaCache
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Component

@Component
class RawMetaCacheRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun save(rawMetaCache: RawMetaCache): RawMetaCache {
        return template.save(rawMetaCache).awaitFirst()
    }

    suspend fun get(cacheId: RawMetaCache.CacheId): RawMetaCache? {
        return template.findById<RawMetaCache>(cacheId).awaitFirstOrNull()
    }
}