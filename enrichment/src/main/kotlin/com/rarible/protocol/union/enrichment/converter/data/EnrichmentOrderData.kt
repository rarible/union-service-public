package com.rarible.protocol.union.enrichment.converter.data

import com.rarible.protocol.union.dto.CollectionIdDto

class EnrichmentOrderData(
    val customCollection: CollectionIdDto? = null
) {

    companion object {

        private val EMPTY = EnrichmentOrderData()
        fun empty() = EMPTY
    }

}