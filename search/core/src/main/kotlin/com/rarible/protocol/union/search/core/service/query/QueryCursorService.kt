package com.rarible.protocol.union.search.core.service.query

import com.rarible.protocol.union.search.core.ElasticActivity
import com.rarible.protocol.union.search.core.model.ActivityCursor
import com.rarible.protocol.union.search.core.model.ActivitySort
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.index.query.TermQueryBuilder
import org.springframework.stereotype.Service

@Service
class QueryCursorService {

    fun applyCursor(query: BoolQueryBuilder, sort: ActivitySort, cursorAsString: String?) {
        if (cursorAsString.isNullOrEmpty()) return
        val cursor = ActivityCursor.fromString(cursorAsString)

        val cursorQuery = BoolQueryBuilder()
        // date <> cursor OR
        cursorQuery.should(
            mustDiffer(ElasticActivity::date.name, cursor.date, sort)
        )
        // date == cursor && blockNumber <> cursor OR
        cursorQuery.shouldAll(
            { this.mustEqual(ElasticActivity::date.name, cursor.date) },
            { this.mustDiffer(ElasticActivity::blockNumber.name, cursor.blockNumber, sort) }
        )
        // date == cursor && blockNumber == cursor && logIndex <> cursor OR
        cursorQuery.shouldAll(
            { this.mustEqual(ElasticActivity::date.name, cursor.date) },
            { this.mustEqual(ElasticActivity::blockNumber.name, cursor.blockNumber) },
            { this.mustDiffer(ElasticActivity::logIndex.name, cursor.logIndex, sort) }
        )
        // date == cursor && blockNumber == cursor && logIndex == cursor && salt <> cursor
        cursorQuery.shouldAll(
            { this.mustEqual(ElasticActivity::date.name, cursor.date) },
            { this.mustEqual(ElasticActivity::blockNumber.name, cursor.blockNumber) },
            { this.mustEqual(ElasticActivity::logIndex.name, cursor.logIndex) },
            { this.mustDiffer(ElasticActivity::salt.name, cursor.salt, sort) }
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

    private fun mustDiffer(fieldName: String, value: Any?, sort: ActivitySort): QueryBuilder? {
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
