package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.enrichment.evaluator.*
import com.rarible.protocol.union.enrichment.model.CurrencyId
import com.rarible.protocol.union.enrichment.model.ShortOrder
import org.springframework.stereotype.Component

@Component
class BestOrderReducer(
    private val currencyService: CurrencyService
) {
    suspend fun reduceSellsByUsd(orders: Map<CurrencyId, ShortOrder>): ShortOrder? {
        return reduceByUsd(orders, BestUsdSellOrderComparator)
    }

    suspend fun reduceBidsByUsd(orders: Map<CurrencyId, ShortOrder>): ShortOrder? {
        return reduceByUsd(orders, BestUsdBidOrderComparator)
    }

    suspend fun reduceSells(orders: List<ShortOrder>): ShortOrder? {
        return reduce(orders, BestSellOrderComparator)
    }

    suspend fun reduceBids(orders: List<ShortOrder>): ShortOrder? {
        return reduce(orders, BestBidOrderComparator)
    }

    private suspend fun reduceByUsd(orders: Map<CurrencyId, ShortOrder>, comparator: BestOrderComparator): ShortOrder? {
        val at = nowMillis()
        val usdEnrichedOrders = orders.map { entity ->
            val order = entity.value
            val currencyId = entity.key
            val rate = currencyService.getRate(order.blockchain, currencyId.address, at).rate

            order.copy(
                makePriceUsd = order.makePrice?.let { makePrice -> makePrice * rate },
                takePriceUsd = order.takePrice?.let { takePrice -> takePrice * rate }
            )
        }
        return usdEnrichedOrders.fold(usdEnrichedOrders.firstOrNull()) { current, update ->
            val bestOrder = current?.let { comparator.compare(current, update) }
            when (bestOrder?.id) {
                current?.id -> current
                update.id -> update
                else -> null
            }
        }
    }

    private fun reduce(orders: List<ShortOrder>, comparator: BestOrderComparator): ShortOrder? {
        return orders.fold(orders.firstOrNull()) { current, update ->
            val bestOrder = current?.let { comparator.compare(current, update) }
            when (bestOrder?.id) {
                current?.id -> current
                update.id -> update
                else -> null
            }
        }
    }
}
