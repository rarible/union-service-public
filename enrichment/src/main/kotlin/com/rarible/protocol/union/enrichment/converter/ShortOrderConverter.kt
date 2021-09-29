package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.model.ShortOrder

object ShortOrderConverter {

    fun convert(order: OrderDto): ShortOrder {

        return ShortOrder(
            blockchain = order.id.blockchain,
            id = order.id.value,
            platform = order.platform.name,
            makeStock = order.makeStock,
            makePriceUsd = order.makePriceUsd,
            takePriceUsd = order.takePriceUsd
        )
    }
}