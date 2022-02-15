package com.rarible.protocol.union.enrichment.meta

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Deprecated("This class is only used to migrate cache_meta database table to new meta cache.")
data class ContentMetaEntry(
    @Id
    val id: String,
    val data: ContentMeta
) {
    companion object {
        const val CACHE_META_TABLE = "cache_meta"
    }
}
