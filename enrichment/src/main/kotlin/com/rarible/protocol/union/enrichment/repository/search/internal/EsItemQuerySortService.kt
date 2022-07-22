package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsActivitySort
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemSort
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsItemQuerySortService {

    fun applySort(builder: NativeSearchQueryBuilder, sort: EsItemSort) {
        val sortOrder = if (sort.latestFirst) SortOrder.DESC else SortOrder.ASC
        builder.sortByField(EsItem::lastUpdatedAt, sortOrder)
        builder.sortByField(EsItem::itemId, SortOrder.ASC)
    }
}
