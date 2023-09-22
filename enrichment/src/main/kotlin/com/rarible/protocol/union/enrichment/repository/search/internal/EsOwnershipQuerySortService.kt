package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.elastic.EsOwnership
import com.rarible.protocol.union.core.model.elastic.EsOwnershipSort
import com.rarible.protocol.union.core.model.elastic.EsOwnershipSort.EARLIEST_FIRST
import com.rarible.protocol.union.core.model.elastic.EsOwnershipSort.HIGHEST_SELL_PRICE_FIRST
import com.rarible.protocol.union.core.model.elastic.EsOwnershipSort.LATEST_FIRST
import com.rarible.protocol.union.core.model.elastic.EsOwnershipSort.LOWEST_SELL_PRICE_FIRST
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsOwnershipQuerySortService {

    fun applySort(builder: NativeSearchQueryBuilder, sort: EsOwnershipSort) {
        when (sort) {
            LATEST_FIRST,
            EARLIEST_FIRST -> sortByLastUpdatedAt(builder, sort)
            HIGHEST_SELL_PRICE_FIRST,
            LOWEST_SELL_PRICE_FIRST -> sortByPrice(builder, sort == HIGHEST_SELL_PRICE_FIRST)
        }
    }

    private fun sortByLastUpdatedAt(builder: NativeSearchQueryBuilder, sort: EsOwnershipSort) {
        val sortOrder = sort.sortOrder
        builder.sortByField(EsOwnership::date, sortOrder)
        builder.sortByField(EsOwnership::ownershipId, sortOrder)
    }

    private fun sortByPrice(builder: NativeSearchQueryBuilder, latestFirst: Boolean) {
        val sortOrder = if (latestFirst) SortOrder.DESC else SortOrder.ASC
        builder.sortByField("_score", sortOrder)
        builder.sortByField(EsOwnership::ownershipId, sortOrder)
    }
}
