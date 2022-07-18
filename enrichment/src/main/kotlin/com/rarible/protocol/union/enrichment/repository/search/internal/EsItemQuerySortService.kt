package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemSort
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder

object EsItemQuerySortService {

    fun NativeSearchQueryBuilder.applySort(sort: EsItemSort) {
        val sortOrder = if (sort.latestFirst) SortOrder.DESC else SortOrder.ASC
        sortByField(EsItem::lastUpdatedAt, sortOrder)
        sortByField(EsItem::itemId, SortOrder.ASC)
    }
}