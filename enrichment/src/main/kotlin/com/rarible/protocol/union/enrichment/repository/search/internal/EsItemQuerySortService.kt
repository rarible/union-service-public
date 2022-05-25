package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemSort
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder

object EsItemQuerySortService {

    fun NativeSearchQueryBuilder.applySort(sort: EsItemSort) {
        val sortOrder = if (sort.latestFirst == true) SortOrder.DESC else SortOrder.ASC
        sortByField(EsItem::lastUpdatedAt.name, sortOrder)
        sortByField(EsItem::itemId.name, SortOrder.ASC)
    }

    private fun NativeSearchQueryBuilder.sortByField(fieldName: String, order: SortOrder) {
        val sort = SortBuilders
            .fieldSort(fieldName)
            .order(order)
        withSort(sort)
    }
}