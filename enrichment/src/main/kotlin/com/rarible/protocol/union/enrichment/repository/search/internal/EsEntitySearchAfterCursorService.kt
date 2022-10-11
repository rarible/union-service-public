package com.rarible.protocol.union.enrichment.repository.search.internal

import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.stereotype.Component

@Component
class EsEntitySearchAfterCursorService {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun tryFixLegacyCursor(cursor: String?): String? {
        if (cursor.isNullOrBlank()) return cursor
        return if (cursor.first().isLetter()) {
            logger.info("Attempt to fix legacy cursor $cursor")
            cursor.substringAfter(":")
        } else {
            cursor
        }
    }

    fun buildSearchAfterClause(cursorAsString: String?): List<Any>? {
        if (cursorAsString.isNullOrEmpty()) return null
        return cursorAsString.split("_")
    }

    fun buildCursor(lastSearchHit: SearchHit<*>?): String? {
        if (lastSearchHit == null) return null
        return lastSearchHit.sortValues.joinToString("_")
    }
}
