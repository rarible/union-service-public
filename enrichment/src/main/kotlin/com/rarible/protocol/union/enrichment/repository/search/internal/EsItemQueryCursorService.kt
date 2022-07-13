package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsActivityCursor
import com.rarible.protocol.union.core.model.EsActivitySort
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemCursor
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.core.model.EsOwnership
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.RangeQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsItemQueryCursorService {

    fun applyCursor(query: BoolQueryBuilder, sort: EsItemSort, cursorAsString: String?) {
        if (cursorAsString.isNullOrEmpty()) return
        val cursor = EsItemCursor.fromString(cursorAsString) ?: return
        val cursorQuery = BoolQueryBuilder()

        // date <> cursor OR
        cursorQuery.should(
            mustDiffer(EsItem::lastUpdatedAt.name, cursor.date, descending = sort.latestFirst)
        )

        // date == cursor AND id <> cursor
        cursorQuery.shouldAll(
            { mustEqual(EsItem::lastUpdatedAt.name, cursor.date) },
            { mustDiffer(EsItem::itemId.name, cursor.itemId, descending = sort.latestFirst) }
        )

        cursorQuery.minimumShouldMatch(1)
        query.must(cursorQuery)
    }
}
