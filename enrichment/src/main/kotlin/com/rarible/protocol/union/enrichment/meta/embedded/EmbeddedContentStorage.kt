package com.rarible.protocol.union.enrichment.meta.embedded

interface EmbeddedContentStorage {

    suspend fun get(id: String): UnionEmbeddedContent?

    suspend fun save(content: UnionEmbeddedContent)

    suspend fun delete(id: String)
}
