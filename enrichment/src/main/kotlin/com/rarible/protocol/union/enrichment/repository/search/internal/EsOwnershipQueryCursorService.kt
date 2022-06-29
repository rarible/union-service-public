package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import org.elasticsearch.index.query.BoolQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsOwnershipQueryCursorService {

    fun applyCursor(query: BoolQueryBuilder, cursorAsString: String?) {
        if (cursorAsString.isNullOrEmpty()) return

        val cursor = DateIdContinuation.parse(cursorAsString) ?: return
        val cursorQuery = BoolQueryBuilder()

        // date <> cursor OR
        cursorQuery.should(
            mustDiffer(EsOwnership::date.name, cursor.date, descending = !cursor.asc)
        )

        // date == cursor AND id <> cursor
        cursorQuery.shouldAll(
            { mustEqual(EsOwnership::date.name, cursor.date) },
            { mustDiffer(EsOwnership::ownershipId.name, cursor.id, descending = !cursor.asc) }
        )

        cursorQuery.minimumShouldMatch(1)
        query.must(cursorQuery)
    }
}
