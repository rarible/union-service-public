package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemCursor
import com.rarible.protocol.union.core.model.EsItemSort
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.RangeQueryBuilder

object EsItemQueryCursorService {

    fun BoolQueryBuilder.applyCursor(sort: EsItemSort, cursor: EsItemCursor?) {
        if (cursor == null) return
        must(
            mustDiffer(EsItem::lastUpdatedAt.name, cursor.date, sort)
        )
        must(
            RangeQueryBuilder(EsItem::itemId.name).gt(cursor.itemId)
        )
    }

    private fun mustDiffer(fieldName: String, value: Any?, sort: EsItemSort): QueryBuilder? {
        return if (value != null) {
            val rangeQuery = RangeQueryBuilder(fieldName)
            if (sort.latestFirst) {
                rangeQuery.lt(value)
            } else {
                rangeQuery.gt(value)
            }
            rangeQuery
        } else null
    }
}
