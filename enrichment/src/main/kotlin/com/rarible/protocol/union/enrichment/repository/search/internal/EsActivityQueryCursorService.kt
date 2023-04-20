package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.elastic.EsActivity
import com.rarible.protocol.union.core.model.elastic.EsActivityCursor
import com.rarible.protocol.union.core.model.elastic.EsActivitySort
import org.elasticsearch.index.query.BoolQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsActivityQueryCursorService {

    fun applyCursor(query: BoolQueryBuilder, sort: EsActivitySort, cursorAsString: String?) {
        if (cursorAsString.isNullOrEmpty()) return
        val cursor = EsActivityCursor.fromString(cursorAsString) ?: return

        val cursorQuery = BoolQueryBuilder()
        // date <> cursor OR
        cursorQuery.should(
            mustDiffer(EsActivity::date.name, cursor.date, sort.latestFirst)
        )
        // date == cursor && blockNumber <> cursor OR
        if (cursor.blockNumber != 0L) {
            cursorQuery.shouldAll(
                { mustEqual(EsActivity::date.name, cursor.date) },
                { mustDiffer(EsActivity::blockNumber.name, cursor.blockNumber, sort.latestFirst) }
            )
        }
        // date == cursor && blockNumber == cursor && logIndex <> cursor OR
        if (cursor.blockNumber != 0L && cursor.logIndex != 0) {
            cursorQuery.shouldAll(
                { mustEqual(EsActivity::date.name, cursor.date) },
                { mustEqual(EsActivity::blockNumber.name, cursor.blockNumber) },
                { mustDiffer(EsActivity::logIndex.name, cursor.logIndex, sort.latestFirst) }
            )
        }
        // date == cursor && blockNumber == cursor && logIndex == cursor && salt <> cursor
        cursorQuery.shouldAll(
            { mustEqual(EsActivity::date.name, cursor.date) },
            { mustEqual(EsActivity::blockNumber.name, cursor.blockNumber) },
            { mustEqual(EsActivity::logIndex.name, cursor.logIndex) },
            { mustDiffer(EsActivity::salt.name, cursor.salt, sort.latestFirst) }
        )

        cursorQuery.minimumShouldMatch(1)
        query.must(cursorQuery)
    }
}
