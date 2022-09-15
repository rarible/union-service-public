package com.rarible.protocol.union.enrichment.repository.search.internal

import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.stereotype.Component

@Component
class EsEntitySearchAfterCursorService {

    fun buildSearchAfterClause(cursorAsString: String?): List<Any>? {
        if (cursorAsString.isNullOrEmpty()) return null
        return cursorAsString.split("_")
    }

    fun buildCursor(lastSearchHit: SearchHit<*>?): String? {
        if (lastSearchHit == null) return null
        return lastSearchHit.sortValues.joinToString("_")
    }
}
