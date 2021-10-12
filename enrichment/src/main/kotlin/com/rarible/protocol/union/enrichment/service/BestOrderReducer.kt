package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.enrichment.evaluator.BestBidOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.BestOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.BestSellOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.BestUsdBidOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.BestUsdSellOrderComparator
import com.rarible.protocol.union.enrichment.model.ShortOrder
import org.springframework.stereotype.Component

@Component
class BestOrderReducer(
        private val currencyService: CurrencyService
) {

    suspend fun reduceSellsByUsd(orders: Map<String, ShortOrder>): ShortOrder? {
        return reduceByUsd(orders, BestUsdSellOrderComparator)
    }

    suspend fun reduceBidsByUsd(orders: Map<String, ShortOrder>): ShortOrder? {
        return reduceByUsd(orders, BestUsdBidOrderComparator)
    }

    suspend fun reduceSells(orders: List<ShortOrder>): ShortOrder? {
        return reduce(orders, BestSellOrderComparator)
    }

    suspend fun reduceBids(orders: List<ShortOrder>): ShortOrder? {
        return reduce(orders, BestBidOrderComparator)
    }

    private suspend fun reduceByUsd(orders: Map<String, ShortOrder>, comparator: BestOrderComparator): ShortOrder? {
        val usdEnrichedOrders = orders.map { entity ->
            val currencyId = entity.key
            val order = entity.value
            val rate = currencyService.getCurrentRate(order.blockchain, currencyId).rate
            order.copy(
                    makePriceUsd = order.makePrice?.let { makePrice -> makePrice * rate },
                    takePriceUsd = order.takePrice?.let { takePrice -> takePrice * rate }
            )
        }
        return reduce(usdEnrichedOrders, comparator)
    }

    private fun reduce(orders: List<ShortOrder>, comparator: BestOrderComparator): ShortOrder? {
        if (orders.isEmpty()) return null
        return orders.reduce { current, next -> comparator.compare(current, next) }
    }
}
