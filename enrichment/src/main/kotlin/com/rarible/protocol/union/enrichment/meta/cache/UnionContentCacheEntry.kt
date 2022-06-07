package com.rarible.protocol.union.enrichment.meta.cache

import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("meta_content_url")
data class UnionContentCacheEntry(
    @Id
    val url: String,
    val type: String,
    val updatedAt: Instant,
    val content: UnionMetaContentProperties
)