package com.rarible.protocol.union.enrichment.custom.collection.fetcher

import com.rarible.protocol.union.core.model.UnionItem

data class CustomCollectionItemBatch(
    val state: String?,
    val items: List<UnionItem>
) {

    companion object {

        fun empty(): CustomCollectionItemBatch {
            return CustomCollectionItemBatch(null, emptyList())
        }
    }

}