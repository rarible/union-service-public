package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.evaluator.BestBidOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.BestOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.BestOrderEvaluator
import com.rarible.protocol.union.enrichment.evaluator.BestSellOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.ItemBestBidOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.ItemBestSellOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.OwnershipBestSellOrderProvider
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortOrder
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.util.bidCurrencyId
import com.rarible.protocol.union.enrichment.util.sellCurrencyId
import org.springframework.stereotype.Component
import java.util.*

@Component
class BestOrderService(
    private val enrichmentOrderService: EnrichmentOrderService,
    private val currencyService: CurrencyService
) {

    // TODO we can return here Full Order if it was fetched - thats allow us to avoid one more query to indexer
    // for update events in ownership/item
    suspend fun updateBestSellOrder(ownership: ShortOwnership, order: OrderDto): ShortOwnership {
        val currencyId = order.sellCurrencyId
        val evaluator = BestOrderEvaluator(
            comparator = BestSellOrderComparator,
            provider = OwnershipBestSellOrderProvider(ownership.id, currencyId, enrichmentOrderService)
        )

        val bestSellOrders = updateCurrencyOrders(ownership.bestSellOrders, order, evaluator, currencyId)

        val updatedOwnership = ownership.copy(bestSellOrders = bestSellOrders)
        return updateBestSellOrder(updatedOwnership)
    }

    suspend fun updateBestSellOrder(ownership: ShortOwnership): ShortOwnership {
        val bestSellOrder = getBestSellOrderInUsd(ownership.bestSellOrders)
        return ownership.copy(bestSellOrder = bestSellOrder)
    }

    suspend fun updateBestSellOrder(item: ShortItem, order: OrderDto): ShortItem {
        val currencyId = order.sellCurrencyId
        val evaluator = BestOrderEvaluator(
            comparator = BestSellOrderComparator,
            provider = ItemBestSellOrderProvider(item.id, currencyId, enrichmentOrderService)
        )

        val bestSellOrders = updateCurrencyOrders(item.bestSellOrders, order, evaluator, currencyId)
        val updatedItem = item.copy(bestSellOrders = bestSellOrders)
        return updateBestSellOrder(updatedItem)
    }

    suspend fun updateBestSellOrder(item: ShortItem): ShortItem {
        val bestSellOrder = getBestSellOrderInUsd(item.bestSellOrders)
        return item.copy(bestSellOrder = bestSellOrder)
    }

    suspend fun updateBestBidOrder(item: ShortItem, order: OrderDto): ShortItem {
        val currencyId = order.bidCurrencyId

        val evaluator = BestOrderEvaluator(
            comparator = BestBidOrderComparator,
            provider = ItemBestBidOrderProvider(item.id, currencyId, enrichmentOrderService)
        )

        val bestBidOrders = updateCurrencyOrders(item.bestBidOrders, order, evaluator, currencyId)
        val updatedItem = item.copy(bestBidOrders = bestBidOrders)

        return updateBestBidOrder(updatedItem)
    }

    suspend fun updateBestBidOrder(item: ShortItem): ShortItem {
        val bestBidOrder = getBestBidOrderInUsd(item.bestBidOrders)
        return item.copy(bestBidOrder = bestBidOrder)
    }

    suspend fun getBestSellOrderInUsd(orders: Map<String, ShortOrder>): ShortOrder? {
        return getBestOrderByUsd(orders, BestSellOrderComparator)
    }

    suspend fun getBestBidOrderInUsd(orders: Map<String, ShortOrder>): ShortOrder? {
        return getBestOrderByUsd(orders, BestBidOrderComparator)
    }

    suspend fun getBestSellOrder(orders: List<ShortOrder>): ShortOrder? {
        return getBestOrder(orders, BestSellOrderComparator)
    }

    suspend fun getBestBidOrder(orders: List<ShortOrder>): ShortOrder? {
        return getBestOrder(orders, BestBidOrderComparator)
    }

    private suspend fun getBestOrderByUsd(
        orders: Map<String, ShortOrder>,
        comparator: BestOrderComparator
    ): ShortOrder? {
        val mappedOrders = orders.values.associateBy { it.id }
        val usdEnrichedOrders = orders.map { entity ->
            val currencyId = entity.key
            val order = entity.value
            val rate = currencyService.getCurrentRate(order.blockchain, currencyId).rate
            order.copy(
                // TODO???
                makePrice = order.makePrice?.let { makePrice -> makePrice * rate },
                takePrice = order.takePrice?.let { takePrice -> takePrice * rate }
            )
        }
        val best = getBestOrder(usdEnrichedOrders, comparator)
        return best?.let { mappedOrders[best.id] }
    }

    private fun getBestOrder(orders: List<ShortOrder>, comparator: BestOrderComparator): ShortOrder? {
        if (orders.isEmpty()) return null
        return orders.reduce { current, next -> comparator.compare(current, next) }
    }

    private suspend fun updateCurrencyOrders(
        orders: Map<String, ShortOrder>,
        updated: OrderDto,
        evaluator: BestOrderEvaluator,
        currencyId: String
    ): Map<String, ShortOrder> {
        val updatedOrders = TreeMap(orders)

        val bestCurrent = orders[currencyId]
        val bestUpdated = evaluator.evaluateBestOrder(bestCurrent, updated)

        bestUpdated?.let { updatedOrders[currencyId] = bestUpdated } ?: updatedOrders.remove(currencyId)

        return updatedOrders
    }

}

