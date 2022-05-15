package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemSort
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsItemQuerySortService {

    fun applySort(builder: NativeSearchQueryBuilder, sort: EsItemSort) {
        val sortOrder = if (sort.latestFirst == true) SortOrder.DESC else SortOrder.ASC
        sort.byId?.run { builder.sortByField(EsItem::itemId.name, SortOrder.ASC) }
    }

    fun applySort(builder: SearchSourceBuilder, sort: EsItemSort) {
        sort.byId?.run { builder.sort(EsItem::itemId.name) }
    }

    private fun NativeSearchQueryBuilder.sortByField(fieldName: String, order: SortOrder) {
        val sort = SortBuilders
            .fieldSort(fieldName)
            .order(order)
        withSort(sort)
    }
}