package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionOrderMatchSell
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

    fun convert(sell: UnionActivity?): ItemLastSale? {
        if (sell == null) {
            return null
        }
        return when (sell) {
            is UnionOrderMatchSell -> ItemLastSale(
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