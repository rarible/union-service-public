package com.rarible.protocol.union.enrichment.util

import com.rarible.protocol.union.core.model.UnionMetaContent

fun sanitizeContent(content: List<UnionMetaContent>): List<UnionMetaContent> {
    return content.mapNotNull {
        if (it.url.isBlank()) {
            null
        } else {
            it.copy(url = it.url.trim())
        }
    }
}
