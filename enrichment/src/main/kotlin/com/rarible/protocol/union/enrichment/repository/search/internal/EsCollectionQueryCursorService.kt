package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.core.model.EsCollectionCursor
import org.elasticsearch.index.query.BoolQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsCollectionQueryCursorService {

    fun applyCursor(query: BoolQueryBuilder, cursorAsString: String?) {
        if (cursorAsString.isNullOrEmpty()) return
        val cursor = EsCollectionCursor.fromString(cursorAsString) ?: return

        val cursorQuery = BoolQueryBuilder()
        // date <> cursor OR
        cursorQuery.should(
            mustDiffer(EsCollection::date.name, cursor.date, descending = true)
        )
        // date == cursor salt <> cursor
        cursorQuery.shouldAll(
            { mustEqual(EsActivity::date.name, cursor.date) },
            { mustDiffer(EsCollection::salt.name, cursor.salt, descending = true) }
        )

        cursorQuery.minimumShouldMatch(1)
        query.must(cursorQuery)
    }
}
