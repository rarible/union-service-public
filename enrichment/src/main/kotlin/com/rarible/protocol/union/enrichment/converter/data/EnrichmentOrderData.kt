package com.rarible.protocol.union.enrichment.converter.data

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.OrderIdDto

class EnrichmentOrderData(
    val customCollections: Map<OrderIdDto, CollectionIdDto> = emptyMap()
) {

    companion object {

        private val EMPTY = EnrichmentOrderData()
        fun empty() = EMPTY
    }
}
