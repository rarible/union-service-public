package com.rarible.protocol.union.enrichment.repository.search.internal

import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.stereotype.Component

@Component
class EsEntitySearchAfterCursorService {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Deprecated("Need to remove")
    fun tryFixLegacyCursor(cursor: String?): String? {
        if (cursor.isNullOrBlank()) return cursor
        var fixed = if (cursor.first().isLetter()) {
            logger.info("Attempt to fix legacy cursor $cursor")
            cursor.substringAfter(":")
        } else {
            cursor
        }
        if (!fixed.contains('_')) {
            fixed += "_A"
        }
        return fixed
    }

    fun buildSearchAfterClause(cursorAsString: String?, expectedSize: Int): List<Any>? {
        if (cursorAsString.isNullOrEmpty()) return null
        val lower = cursorAsString.lowercase()
        if (lower.startsWith("null") || lower.startsWith("undefined")) return null
        val result = cursorAsString.split("_")
        if (result.size != expectedSize) {
            logger.warn("Invalid cursor: $cursorAsString")
            return null
        }
        return result
    }

    fun buildCursor(lastSearchHit: SearchHit<*>?): String? {
        if (lastSearchHit == null) return null
        return lastSearchHit.sortValues.joinToString("_")
    }
}
