package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.EsOwnershipSort
import com.rarible.protocol.union.core.model.EsOwnershipSort.EARLIEST_FIRST
import com.rarible.protocol.union.core.model.EsOwnershipSort.HIGHEST_SELL_PRICE_FIRST
import com.rarible.protocol.union.core.model.EsOwnershipSort.LATEST_FIRST
import com.rarible.protocol.union.core.model.EsOwnershipSort.LOWEST_SELL_PRICE_FIRST
import com.rarible.protocol.union.core.service.CurrencyService
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsOwnershipQuerySortService {

    fun applySort(builder: NativeSearchQueryBuilder, sort: EsOwnershipSort) {
        when (sort) {
            LATEST_FIRST,
            EARLIEST_FIRST -> sortByLastUpdatedAt(builder, sort == LATEST_FIRST)
            HIGHEST_SELL_PRICE_FIRST,
            LOWEST_SELL_PRICE_FIRST -> sortByPrice(builder, sort == HIGHEST_SELL_PRICE_FIRST)
        }
    }

    private fun sortByLastUpdatedAt(builder: NativeSearchQueryBuilder, latestFirst: Boolean) {
        val sortOrder = if (latestFirst) SortOrder.DESC else SortOrder.ASC
        builder.sortByField(EsOwnership::date, sortOrder)
        builder.sortByField(EsOwnership::ownershipId, SortOrder.ASC)
    }

    private fun sortByPrice(builder: NativeSearchQueryBuilder, latestFirst: Boolean) {
        val sortOrder = if (latestFirst) SortOrder.DESC else SortOrder.ASC
        builder.sortByField("_score", sortOrder)
        builder.sortByField(EsOwnership::ownershipId, SortOrder.ASC)
    }
}
