package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsActivitySort
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsActivityQuerySortService {

    fun applySort(builder: NativeSearchQueryBuilder, sort: EsActivitySort) {
        val sortOrder = if (sort.latestFirst) SortOrder.DESC else SortOrder.ASC
        builder.sortByField(EsActivity::date.name, sortOrder)
        builder.sortByField(EsActivity::blockNumber.name, sortOrder)
        builder.sortByField(EsActivity::logIndex.name, sortOrder)
        builder.sortByField(EsActivity::salt.name, sortOrder)
    }
}