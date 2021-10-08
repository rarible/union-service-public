package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.converter.CurrencyIdConverter
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
    suspend fun getBestSellOrder(ownership: ShortOwnership, order: OrderDto): ShortOwnership {
        val currencyId = CurrencyIdConverter.convert(order.id.blockchain, order.take.type)
        val bestSellOrders = HashMap(ownership.bestSellOrders)

        val bestOrderEvaluator = BestOrderEvaluator(
            comparator = BestSellOrderComparator,
            provider = OwnershipBestSellOrderProvider(ownership.id, currencyId, enrichmentOrderService)
        )
        val bestCurrent = bestSellOrders[currencyId]
        val bestUpdated = bestOrderEvaluator.evaluateBestOrder(bestCurrent, order)
        if (bestUpdated != null) bestSellOrders[currencyId] = bestUpdated else bestSellOrders.remove(currencyId)

        val bestSellOrder = bestUsdOrderReducer.reduceSellsByUsd(bestSellOrders)

        return ownership.copy(bestSellOrder = bestSellOrder, bestSellOrders = bestSellOrders)
    }

    suspend fun getBestSellOrder(item: ShortItem, order: OrderDto): ShortItem {
        val currencyId = order.sellCurrencyId
        val bestSellOrders = HashMap(item.bestSellOrders)

        val bestOrderEvaluator = BestOrderEvaluator(
            comparator = BestSellOrderComparator,
            provider = ItemBestSellOrderProvider(item.id, currencyId, enrichmentOrderService)
        )

        val bestCurrent = bestSellOrders[currencyId]
        val bestUpdated = bestOrderEvaluator.evaluateBestOrder(bestCurrent, order)
        if (bestUpdated != null) bestSellOrders[currencyId] = bestUpdated else bestSellOrders.remove(currencyId)

        val bestSellOrder = bestUsdOrderReducer.reduceBidsByUsd(bestSellOrders)
        return item.copy(bestSellOrder = bestSellOrder, bestSellOrders = bestSellOrders)
    }

    suspend fun getBestBidOrder(item: ShortItem, order: OrderDto): ShortItem {
        val currencyId = order.bidCurrencyId
        val bestBidOrders = HashMap(item.bestBidOrders)

        val bestOrderEvaluator = BestOrderEvaluator(
            comparator = BestBidOrderComparator,
            provider = ItemBestBidOrderProvider(item.id, currencyId, enrichmentOrderService)
        )

        val bestCurrent = bestBidOrders[currencyId]
        val bestUpdated = bestOrderEvaluator.evaluateBestOrder(bestCurrent, order)
        if (bestUpdated != null) bestBidOrders[currencyId] = bestUpdated else bestBidOrders.remove(currencyId)

        val bestBidOrder = bestUsdOrderReducer.reduceBidsByUsd(bestBidOrders)
        return item.copy(bestBidOrder = bestBidOrder, bestBidOrders = bestBidOrders)
    }
}

