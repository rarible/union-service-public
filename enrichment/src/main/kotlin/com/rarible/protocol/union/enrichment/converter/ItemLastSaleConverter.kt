package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionActivityDto
import com.rarible.protocol.union.core.model.UnionOrderMatchSellDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.enrichment.model.ItemLastSale

object ItemLastSaleConverter {

    @Deprecated("keep with UnionActivity only")
    fun convert(sell: ActivityDto?): ItemLastSale? {
        if (sell == null) {
            return null
        }
        return when (sell) {
            is OrderMatchSellDto -> ItemLastSale(
                date = sell.date,
                seller = sell.seller,
                buyer = sell.buyer,
                value = sell.nft.value,
                currency = sell.payment.type,
                price = sell.price
            )

            else -> null
        }
    }

    fun convert(sell: UnionActivityDto?): ItemLastSale? {
        if (sell == null) {
            return null
        }
        return when (sell) {
            is UnionOrderMatchSellDto -> ItemLastSale(
                date = sell.date,
                seller = sell.seller,
                buyer = sell.buyer,
                value = sell.nft.value,
                // Currency can't have enrichment data
                currency = AssetDtoConverter.convert(sell.payment.type),
                price = sell.price
            )

            else -> null
        }
    }

}