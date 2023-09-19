package com.rarible.protocol.union.core.model.elastic

import org.elasticsearch.search.sort.SortOrder

enum class EsOwnershipSort(override val sortOrder: SortOrder) : OrderedSort {
    LATEST_FIRST(SortOrder.DESC),
    EARLIEST_FIRST(SortOrder.ASC),
    HIGHEST_SELL_PRICE_FIRST(SortOrder.ASC),
    LOWEST_SELL_PRICE_FIRST(SortOrder.ASC);

    companion object {
        val DEFAULT = LATEST_FIRST
    }
}
