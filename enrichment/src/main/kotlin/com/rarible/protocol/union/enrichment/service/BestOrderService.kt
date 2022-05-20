package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.evaluator.BestBidOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.BestBidOrderOwner
import com.rarible.protocol.union.enrichment.evaluator.BestOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.BestOrderEvaluator
import com.rarible.protocol.union.enrichment.evaluator.BestOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.BestPreferredOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.BestSellOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.BestSellOrderOwner
import com.rarible.protocol.union.enrichment.evaluator.CollectionBestBidOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.CollectionBestSellOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.ItemBestBidOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.ItemBestSellOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.OwnershipBestSellOrderProvider
import com.rarible.protocol.union.enrichment.model.ShortCollection
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

    //---------------------- Ownership ----------------------//
    suspend fun updateBestSellOrder(ownership: ShortOwnership, order: OrderDto): ShortOwnership {
        val provider = OwnershipBestSellOrderProvider(ownership.id, enrichmentOrderService)
        return updateBestSellOrder(ownership, provider, order)
    }

    suspend fun updateBestOrders(ownership: ShortOwnership): ShortOwnership {
        return updateBestSellOrder(ownership)
    }

    //---------------------- Collection ---------------------//
    suspend fun updateBestSellOrder(collection: ShortCollection, order: OrderDto): ShortCollection {
        val provider = CollectionBestSellOrderProvider(collection.id, enrichmentOrderService)
        return updateBestSellOrder(collection, provider, order)
    }

    suspend fun updateBestBidOrder(collection: ShortCollection, order: OrderDto): ShortCollection {
        val provider = CollectionBestBidOrderProvider(collection.id, enrichmentOrderService)
        return updateBestBidOrder(collection, provider, order)
    }

    suspend fun updateBestOrders(collection: ShortCollection): ShortCollection {
        return updateBestBidOrder(updateBestSellOrder(collection))
    }

    //------------------------- Item ------------------------//
    suspend fun updateBestSellOrder(item: ShortItem, order: OrderDto): ShortItem {
        val provider = ItemBestSellOrderProvider(item.id, enrichmentOrderService)
        return updateBestSellOrder(item, provider, order)
    }

    suspend fun updateBestBidOrder(item: ShortItem, order: OrderDto): ShortItem {
        val provider = ItemBestBidOrderProvider(item.id, enrichmentOrderService)
        return updateBestBidOrder(item, provider, order)
    }

    suspend fun updateBestOrders(item: ShortItem): ShortItem {
        return updateBestBidOrder(updateBestSellOrder(item))
    }

    //------------------------- USD -------------------------//
    suspend fun getBestSellOrderInUsd(orders: Map<String, ShortOrder>): ShortOrder? {
        return getBestOrderByUsd(orders, BestSellOrderComparator)
    }

    suspend fun getBestBidOrderInUsd(orders: Map<String, ShortOrder>): ShortOrder? {
        return getBestOrderByUsd(orders, BestBidOrderComparator)
    }

    private suspend fun getBestOrderByUsd(
        orders: Map<String, ShortOrder>,
        comparator: BestOrderComparator
    ): ShortOrder? {
        val mappedOrders = orders.values.associateBy { it.id }
        val usdEnrichedOrders = orders.map { entity ->
            val currencyId = entity.key
            val order = entity.value
            val rate = currencyService.getCurrentRate(order.blockchain, currencyId)?.rate
            order.copy(
                // If we have orders which are made in not supports USD conversion,
                // they will have NULL prices and if current order exists, it won't be replaced
                makePrice = order.makePrice?.let { makePrice -> rate?.let { makePrice * rate } },
                takePrice = order.takePrice?.let { takePrice -> rate?.let { takePrice * rate } }
            )
        }
        val best = getBestOrder(usdEnrichedOrders, comparator)
        return best?.let { mappedOrders[best.id] }
    }

    private fun getBestOrder(orders: List<ShortOrder>, comparator: BestOrderComparator): ShortOrder? {
        if (orders.isEmpty()) return null
        val preferredOrderComparator = BestPreferredOrderComparator(comparator)
        return orders.reduce { current, next -> preferredOrderComparator.compare(current, next) }
    }

    private suspend fun updateCurrencyOrders(
        orders: Map<String, ShortOrder>,
        updated: OrderDto,
        evaluator: BestOrderEvaluator
    ): Map<String, ShortOrder> {
        val currencyId = evaluator.currencyId
        val updatedOrders = TreeMap(orders)

        val bestCurrent = orders[currencyId]
        val bestUpdated = evaluator.evaluateBestOrder(bestCurrent, updated)

        bestUpdated?.let { updatedOrders[currencyId] = bestUpdated } ?: updatedOrders.remove(currencyId)

        return updatedOrders
    }

    //--------------------- Best bid/sell -------------------//
    private suspend fun <T : BestSellOrderOwner<T>> updateBestSellOrder(
        bestSellOwner: T,
        bestOrderProvider: BestOrderProvider<T>,
        order: OrderDto
    ): T {
        val currencyId = order.sellCurrencyId
        val evaluator = BestOrderEvaluator(
            comparator = BestSellOrderComparator,
            provider = bestOrderProvider,
            currencyId = currencyId
        )

        val bestOrders = updateCurrencyOrders(bestSellOwner.bestSellOrders, order, evaluator)

        val updatedOwner = bestSellOwner.withBestSellOrders(bestOrders)
        return updateBestSellOrder(updatedOwner)
    }

    private suspend fun <T : BestBidOrderOwner<T>> updateBestBidOrder(
        bestBidOwner: T,
        bestOrderProvider: BestOrderProvider<T>,
        order: OrderDto
    ): T {
        val currencyId = order.bidCurrencyId
        val evaluator = BestOrderEvaluator(
            comparator = BestBidOrderComparator,
            provider = bestOrderProvider,
            currencyId = currencyId
        )

        val bestOrders = updateCurrencyOrders(bestBidOwner.bestBidOrders, order, evaluator)

        val updatedOwner = bestBidOwner.withBestBidOrders(bestOrders)
        return updateBestBidOrder(updatedOwner)
    }

    private suspend fun <T : BestSellOrderOwner<T>> updateBestSellOrder(owner: T): T {
        val bestSellOrder = getBestSellOrderInUsd(owner.bestSellOrders)
        return owner.withBestSellOrder(bestSellOrder)
    }

    private suspend fun <T : BestBidOrderOwner<T>> updateBestBidOrder(owner: T): T {
        val bestSellOrder = getBestBidOrderInUsd(owner.bestBidOrders)
        return owner.withBestBidOrder(bestSellOrder)
    }

}

