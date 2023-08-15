package com.rarible.protocol.union.core.model.elastic

import org.elasticsearch.search.sort.SortOrder

enum class EsItemSort(val sortOrder: SortOrder) {
    LATEST_FIRST(SortOrder.DESC),
    EARLIEST_FIRST(SortOrder.ASC),
    HIGHEST_SELL_PRICE_FIRST(SortOrder.DESC),
    LOWEST_SELL_PRICE_FIRST(SortOrder.ASC),
    HIGHEST_BID_PRICE_FIRST(SortOrder.DESC),
    LOWEST_BID_PRICE_FIRST(SortOrder.ASC);

    companion object {
        val DEFAULT = LATEST_FIRST
    }
}
