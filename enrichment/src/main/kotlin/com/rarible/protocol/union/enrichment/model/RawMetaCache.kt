package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.enrichment.meta.MetaSource
import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Duration
import java.time.Instant

@Document(RawMetaCache.COLLECTION)
data class RawMetaCache(
    val entityId: String,
    val source: MetaSource,
    val data: String,
    val createdAt: Instant,
) {
    constructor(
        id: CacheId,
        data: String,
        createdAt: Instant,
    ) : this(id.entityId, id.source, data, createdAt)

    @Transient
    private val _id: CacheId = CacheId(source, entityId)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: CacheId
        get() = _id
        set(_) {}

    fun hasExpired(ttl: Duration): Boolean {
        return createdAt.plus(ttl) < Instant.now()
    }

    data class CacheId(
        val source: MetaSource,
        val entityId: String,
    )

    companion object {
        const val COLLECTION = "raw_meta_cache"
    }
}
