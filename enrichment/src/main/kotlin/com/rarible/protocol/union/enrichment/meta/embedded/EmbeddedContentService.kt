package com.rarible.protocol.union.enrichment.meta.embedded

import org.springframework.stereotype.Component

@Component
class EmbeddedContentService(
    private val embeddedContentStorage: EmbeddedContentStorage
) {

    suspend fun get(id: String): UnionEmbeddedContent? {
        return embeddedContentStorage.get(id)
    }

    suspend fun save(content: UnionEmbeddedContent): UnionEmbeddedContent {
        embeddedContentStorage.save(content)
        return content
    }

    suspend fun delete(id: String) {
        embeddedContentStorage.delete(id)
    }
}