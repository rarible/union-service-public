package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.elastic.EsActivity
import com.rarible.protocol.union.core.model.elastic.EsActivitySort
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsActivityQuerySortService {

    fun applySort(builder: NativeSearchQueryBuilder, sort: EsActivitySort) {
        val sortOrder = sort.sortOrder
        builder.sortByField(EsActivity::date, sortOrder)
        builder.sortByField(EsActivity::blockNumber, sortOrder)
        builder.sortByField(EsActivity::logIndex, sortOrder)
        builder.sortByField(EsActivity::salt, sortOrder)
    }
}
