package com.rarible.protocol.union.core.elasticsearch.repository.internal

import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsActivityCursor
import com.rarible.protocol.union.core.model.EsActivitySort
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.index.query.TermQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsActivityQueryCursorService {

    fun applyCursor(query: BoolQueryBuilder, sort: EsActivitySort, cursorAsString: String?) {
        if (cursorAsString.isNullOrEmpty()) return
        val cursor = EsActivityCursor.fromString(cursorAsString) ?: return

        val cursorQuery = BoolQueryBuilder()
        // date <> cursor OR
        cursorQuery.should(
            mustDiffer(EsActivity::date.name, cursor.date, sort)
        )
        // date == cursor && blockNumber <> cursor OR
        if (cursor.blockNumber != null) {
            cursorQuery.shouldAll(
                { this.mustEqual(EsActivity::date.name, cursor.date) },
                { this.mustDiffer(EsActivity::blockNumber.name, cursor.blockNumber, sort) }
            )
        }
        // date == cursor && blockNumber == cursor && logIndex <> cursor OR
        if (cursor.blockNumber != null && cursor.logIndex != null) {
            cursorQuery.shouldAll(
                { this.mustEqual(EsActivity::date.name, cursor.date) },
                { this.mustEqual(EsActivity::blockNumber.name, cursor.blockNumber) },
                { this.mustDiffer(EsActivity::logIndex.name, cursor.logIndex, sort) }
            )
        }
        // date == cursor && blockNumber == cursor && logIndex == cursor && salt <> cursor
        cursorQuery.shouldAll(
            { this.mustEqual(EsActivity::date.name, cursor.date) },
            { this.mustEqual(EsActivity::blockNumber.name, cursor.blockNumber) },
            { this.mustEqual(EsActivity::logIndex.name, cursor.logIndex) },
            { this.mustDiffer(EsActivity::salt.name, cursor.salt, sort) }
        )

        cursorQuery.minimumShouldMatch(1)
        query.must(cursorQuery)
    }

    private fun BoolQueryBuilder.shouldAll(vararg queryBuilders: () -> QueryBuilder?) {
        val mustAll = BoolQueryBuilder()
        queryBuilders.forEach {
            val query = it()
            if (query != null) {
                mustAll.must(query)
            }
        }
        this.should(mustAll)
    }

    private fun mustEqual(fieldName: String, value: Any?): QueryBuilder? {
        return if (value != null) {
            TermQueryBuilder(fieldName, value)
        } else null
    }

    private fun mustDiffer(fieldName: String, value: Any?, sort: EsActivitySort): QueryBuilder? {
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
