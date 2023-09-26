package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.model.elastic.EsItemSort
import com.rarible.protocol.union.core.model.elastic.EsItemSortType.EARLIEST_FIRST
import com.rarible.protocol.union.core.model.elastic.EsItemSortType.HIGHEST_BID_PRICE_FIRST
import com.rarible.protocol.union.core.model.elastic.EsItemSortType.HIGHEST_SELL_PRICE_FIRST
import com.rarible.protocol.union.core.model.elastic.EsItemSortType.LATEST_FIRST
import com.rarible.protocol.union.core.model.elastic.EsItemSortType.LOWEST_BID_PRICE_FIRST
import com.rarible.protocol.union.core.model.elastic.EsItemSortType.LOWEST_SELL_PRICE_FIRST
import com.rarible.protocol.union.core.model.elastic.EsItemSortType.TRAIT
import com.rarible.protocol.union.core.model.elastic.SortType
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.NestedSortBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsItemQuerySortService {

    fun applySort(builder: NativeSearchQueryBuilder, sort: EsItemSort) {
        when (sort.type) {
            LATEST_FIRST,
            EARLIEST_FIRST -> sortByLastUpdatedAt(builder, sort.sortOrder)
            HIGHEST_SELL_PRICE_FIRST,
            LOWEST_SELL_PRICE_FIRST -> sortByPrice(builder, sort.sortOrder)
            HIGHEST_BID_PRICE_FIRST,
            LOWEST_BID_PRICE_FIRST -> sortByPrice(builder, sort.sortOrder)
            TRAIT -> sortByTrait(builder, sort)
        }
    }

    private fun sortByTrait(builder: NativeSearchQueryBuilder, sort: EsItemSort) {
        val traitSort = sort.traitSort
        if (traitSort != null) {
            val field = if (traitSort.sortType == SortType.NUMERIC) "numeric" else "raw"
            builder.withSort(
                SortBuilders.fieldSort("traits.value.$field")
                    .setNestedSort(
                        NestedSortBuilder("traits").setFilter(
                            QueryBuilders.termQuery(
                                "traits.key.raw",
                                traitSort.key
                            )
                        )
                    )
                    .order(sort.sortOrder)
            )
        }
        builder.sortByField(EsItem::itemId, sort.sortOrder)
    }

    private fun sortByLastUpdatedAt(builder: NativeSearchQueryBuilder, sortOrder: SortOrder) {
        builder.sortByField(EsItem::lastUpdatedAt, sortOrder)
        builder.sortByField(EsItem::itemId, sortOrder)
    }

    private fun sortByPrice(builder: NativeSearchQueryBuilder, sortOrder: SortOrder) {
        builder.sortByField("_score", sortOrder)
        builder.sortByField(EsItem::itemId, sortOrder)
    }
}
