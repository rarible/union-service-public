package com.rarible.protocol.union.enrichment.converter.data

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.model.ShortOwnership

data class EnrichmentOwnershipData(
    val shortOwnership: ShortOwnership? = null,
    val orders: Map<OrderIdDto, OrderDto> = emptyMap(),
    val customCollection: CollectionIdDto? = null
) {

    companion object {

        private val EMPTY = EnrichmentOwnershipData()
        fun empty() = EMPTY
    }
}