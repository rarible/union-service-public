package com.rarible.protocol.union.core.model.elastic

import org.elasticsearch.search.sort.SortOrder

data class EsActivitySort(
    val latestFirst: Boolean,
) : OrderedSort {
    override val sortOrder: SortOrder
        get() = if (latestFirst) SortOrder.DESC else SortOrder.ASC
}
