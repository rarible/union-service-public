package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.evaluator.*
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.util.bidCurrencyId
import com.rarible.protocol.union.enrichment.util.sellCurrencyId
import org.springframework.stereotype.Component

@Component
class BestOrderService(
    private val enrichmentOrderService: EnrichmentOrderService,
    private val bestUsdOrderReducer: BestOrderReducer
) {
    // TODO we can return here Full Order if it was fetched - thats allow us to avoid one more query to indexer
    // for update events in ownership/item
    suspend fun getBestSellOrder(ownership: ShortOwnership, order: OrderDto? = null): ShortOwnership {
        val bestSellOrders = HashMap(ownership.bestSellOrders)

        if (order != null) {
            val currencyId = order.sellCurrencyId

            val bestOrderEvaluator = BestOrderEvaluator(
                comparator = BestSellOrderComparator,
                provider = OwnershipBestSellOrderProvider(ownership.id, currencyId, enrichmentOrderService)
            )
            val bestCurrent = bestSellOrders[currencyId]
            val bestUpdated = bestOrderEvaluator.evaluateBestOrder(bestCurrent, order)
            if (bestUpdated != null) bestSellOrders[currencyId] = bestUpdated else bestSellOrders.remove(currencyId)
        }
        val bestSellOrder = bestUsdOrderReducer.reduceSellsByUsd(bestSellOrders)
        return ownership.copy(bestSellOrder = bestSellOrder, bestSellOrders = bestSellOrders)
    }

    suspend fun getBestSellOrder(item: ShortItem, order: OrderDto? = null): ShortItem {
        val bestSellOrders = HashMap(item.bestSellOrders)

        if (order != null) {
            val currencyId = order.sellCurrencyId

            val bestOrderEvaluator = BestOrderEvaluator(
                comparator = BestSellOrderComparator,
                provider = ItemBestSellOrderProvider(item.id, currencyId, enrichmentOrderService)
            )
            val bestCurrent = bestSellOrders[currencyId]
            val bestUpdated = bestOrderEvaluator.evaluateBestOrder(bestCurrent, order)
            if (bestUpdated != null) bestSellOrders[currencyId] = bestUpdated else bestSellOrders.remove(currencyId)
        }
        val bestSellOrder = bestUsdOrderReducer.reduceBidsByUsd(bestSellOrders)
        return item.copy(bestSellOrder = bestSellOrder, bestSellOrders = bestSellOrders)
    }

    suspend fun getBestBidOrder(item: ShortItem, order: OrderDto? = null): ShortItem {
        val bestBidOrders = HashMap(item.bestBidOrders)

        if (order != null) {
            val currencyId = order.bidCurrencyId

            val bestOrderEvaluator = BestOrderEvaluator(
                comparator = BestBidOrderComparator,
                provider = ItemBestBidOrderProvider(item.id, currencyId, enrichmentOrderService)
            )
            val bestCurrent = bestBidOrders[currencyId]
            val bestUpdated = bestOrderEvaluator.evaluateBestOrder(bestCurrent, order)
            if (bestUpdated != null) bestBidOrders[currencyId] = bestUpdated else bestBidOrders.remove(currencyId)
        }
        val bestBidOrder = bestUsdOrderReducer.reduceBidsByUsd(bestBidOrders)
        return item.copy(bestBidOrder = bestBidOrder, bestBidOrders = bestBidOrders)
    }
}

