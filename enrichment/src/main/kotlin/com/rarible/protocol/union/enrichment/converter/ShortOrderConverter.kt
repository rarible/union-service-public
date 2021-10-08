package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.enrichment.model.ShortOrder

object ShortOrderConverter {

    fun convert(order: OrderDto): ShortOrder {

        return ShortOrder(
            blockchain = order.id.blockchain,
            id = order.id.value,
            platform = order.platform.name,
            // We expect here orders with integer value of makeStock since there should be only NFTs
            makeStock =  order.makeStock.toBigInteger(),

            makePrice = order.makePrice,
            takePrice = order.takePrice,

            makePriceUsd = order.makePriceUsd,
            takePriceUsd = order.takePriceUsd
        )
    }
}
