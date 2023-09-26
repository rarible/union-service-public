package com.rarible.protocol.union.core.model.elastic

import org.elasticsearch.search.sort.SortOrder

data class EsItemSort(
    val type: EsItemSortType,
    val traitSort: TraitSort? = null,
) : OrderedSort {
    override val sortOrder: SortOrder
        get() = if (type == EsItemSortType.TRAIT) {
            traitSort?.sortOrder ?: EsItemSortType.TRAIT.sortOrder
        } else {
            type.sortOrder
        }

    companion object {
        val DEFAULT = EsItemSort(type = EsItemSortType.DEFAULT)
    }
}

data class TraitSort(
    val key: String,
    val sortOrder: SortOrder,
    val sortType: SortType,
)

enum class EsItemSortType(override val sortOrder: SortOrder) : OrderedSort {
    LATEST_FIRST(SortOrder.DESC),
    EARLIEST_FIRST(SortOrder.ASC),
    HIGHEST_SELL_PRICE_FIRST(SortOrder.DESC),
    LOWEST_SELL_PRICE_FIRST(SortOrder.ASC),
    HIGHEST_BID_PRICE_FIRST(SortOrder.DESC),
    LOWEST_BID_PRICE_FIRST(SortOrder.ASC),
    TRAIT(SortOrder.ASC);

    companion object {
        val DEFAULT = LATEST_FIRST
    }
}
