package com.rarible.protocol.union.core.model.elastic

import org.elasticsearch.search.sort.SortOrder

interface OrderedSort {
    val sortOrder: SortOrder
}
