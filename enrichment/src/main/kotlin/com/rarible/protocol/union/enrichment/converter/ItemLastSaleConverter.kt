package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionOrderMatchSell
import com.rarible.protocol.union.enrichment.model.EnrichmentActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentOrderMatchSell
import com.rarible.protocol.union.enrichment.model.ItemLastSale

object ItemLastSaleConverter {

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

    fun convert(sell: EnrichmentActivity?): ItemLastSale? {
        if (sell == null) {
            return null
        }
        return when (sell) {
            is EnrichmentOrderMatchSell -> ItemLastSale(
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