package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.enrichment.evaluator.BestBidOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.BestBidOrderOwner
import com.rarible.protocol.union.enrichment.evaluator.BestOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.BestOrderEvaluator
import com.rarible.protocol.union.enrichment.evaluator.BestOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.BestOrderProviderFactory
import com.rarible.protocol.union.enrichment.evaluator.BestSellOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.BestSellOrderOwner
import com.rarible.protocol.union.enrichment.evaluator.CollectionBestBidOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.CollectionBestSellOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.ItemBestBidOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.ItemBestSellOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.OwnershipBestSellOrderProvider
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.OriginOrders
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortOrder
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import org.springframework.stereotype.Component
import java.util.TreeMap

@Component
class BestOrderService(
    private val enrichmentOrderService: EnrichmentOrderService,
    private val currencyService: CurrencyService,
    private val ff: FeatureFlagsProperties
) {

    //---------------------- Ownership ----------------------//
    suspend fun updateBestSellOrder(
        ownership: ShortOwnership,
        order: UnionOrder,
        origins: List<String>
    ): ShortOwnership {
        val providerFactory = OwnershipBestSellOrderProvider.Factory(ownership.id, enrichmentOrderService)
        val originOrders = updateOriginSell(ownership.originOrders, order, origins, providerFactory)
        val updated = updateBestSell(ownership, providerFactory.create(null), order)
        return updated.copy(originOrders = originOrders)
    }

    suspend fun updateBestOrders(ownership: ShortOwnership): ShortOwnership {
        val originBestOrders = refreshBestOrders(ownership.originOrders)
        return refreshBestSellOrder(ownership)
            .copy(originOrders = originBestOrders)
    }

    //---------------------- Collection ---------------------//
    suspend fun updateBestSellOrder(
        collection: EnrichmentCollection,
        order: UnionOrder,
        origins: List<String>
    ): EnrichmentCollection {
        val providerFactory = CollectionBestSellOrderProvider.Factory(collection.id, enrichmentOrderService)
        val originOrders = updateOriginSell(collection.originOrders, order, origins, providerFactory)
        val updated = updateBestSell(collection, providerFactory.create(null), order)
        return updated.copy(originOrders = originOrders)
    }

    suspend fun updateBestBidOrder(
        collection: EnrichmentCollection,
        order: UnionOrder,
        origins: List<String>
    ): EnrichmentCollection {
        val providerFactory = CollectionBestBidOrderProvider.Factory(collection.id, enrichmentOrderService)
        val originOrders = updateOriginBid(collection.originOrders, order, origins, providerFactory)
        val updated = updateBestBid(collection, providerFactory.create(null), order)
        return updated.copy(originOrders = originOrders)
    }

    suspend fun updateBestOrders(collection: EnrichmentCollection): EnrichmentCollection {
        val originBestOrders = refreshBestOrders(collection.originOrders)
        return refreshBestBidOrder(refreshBestSellOrder(collection))
            .copy(originOrders = originBestOrders)
    }

    //------------------------- Item ------------------------//
    suspend fun updateBestSellOrder(
        item: ShortItem,
        order: UnionOrder,
        origins: List<String>
    ): ShortItem {
        val providerFactory = ItemBestSellOrderProvider.Factory(item, enrichmentOrderService, ff.enablePoolOrders)
        val originOrders = updateOriginSell(item.originOrders, order, origins, providerFactory)
        val updated = updateBestSell(item, providerFactory.create(null), order)
        return updated.copy(originOrders = originOrders)
    }

    suspend fun updateBestBidOrder(
        item: ShortItem, order: UnionOrder,
        origins: List<String>
    ): ShortItem {
        val providerFactory = ItemBestBidOrderProvider.Factory(item, enrichmentOrderService)
        val originOrders = updateOriginBid(item.originOrders, order, origins, providerFactory)
        val updated = updateBestBid(item, providerFactory.create(null), order)
        return updated.copy(originOrders = originOrders)
    }

    suspend fun updateBestOrders(item: ShortItem): ShortItem {
        val originBestOrders = refreshBestOrders(item.originOrders)
        return refreshBestBidOrder(refreshBestSellOrder(item))
            .copy(originOrders = originBestOrders)
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
        return orders.reduce { current, next -> comparator.compare(current, next) }
    }

    private suspend fun updateCurrencyOrders(
        orders: Map<String, ShortOrder>,
        updated: UnionOrder,
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
    private suspend fun <T : BestSellOrderOwner<T>> updateBestSell(
        bestSellOwner: T,
        bestOrderProvider: BestOrderProvider<*>,
        order: UnionOrder
    ): T {
        val currencyId = order.getSellCurrencyId()
        val evaluator = BestOrderEvaluator(
            comparator = BestSellOrderComparator,
            provider = bestOrderProvider,
            currencyId = currencyId
        )

        val bestOrders = updateCurrencyOrders(bestSellOwner.bestSellOrders, order, evaluator)

        val updatedOwner = bestSellOwner.withBestSellOrders(bestOrders)
        return refreshBestSellOrder(updatedOwner)
    }

    private suspend fun <T : BestBidOrderOwner<T>> updateBestBid(
        bestBidOwner: T,
        bestOrderProvider: BestOrderProvider<*>,
        order: UnionOrder
    ): T {
        val currencyId = order.getBidCurrencyId()!!
        val evaluator = BestOrderEvaluator(
            comparator = BestBidOrderComparator,
            provider = bestOrderProvider,
            currencyId = currencyId
        )

        val bestOrders = updateCurrencyOrders(bestBidOwner.bestBidOrders, order, evaluator)

        val updatedOwner = bestBidOwner.withBestBidOrders(bestOrders)
        return refreshBestBidOrder(updatedOwner)
    }

    private suspend fun refreshBestOrders(originOrders: Set<OriginOrders>): Set<OriginOrders> {
        return originOrders.map { refreshBestBidOrder(refreshBestSellOrder(it)) }.toSet()
    }

    private suspend fun <T : BestSellOrderOwner<T>> refreshBestSellOrder(owner: T): T {
        val bestSellOrder = getBestSellOrderInUsd(owner.bestSellOrders)
        return owner.withBestSellOrder(bestSellOrder)
    }

    private suspend fun <T : BestBidOrderOwner<T>> refreshBestBidOrder(owner: T): T {
        val bestSellOrder = getBestBidOrderInUsd(owner.bestBidOrders)
        return owner.withBestBidOrder(bestSellOrder)
    }

    private suspend fun updateOriginSell(
        originOrders: Set<OriginOrders>,
        order: UnionOrder,
        origins: List<String>,
        providerFactory: BestOrderProviderFactory<*>
    ): Set<OriginOrders> {
        return updateOriginOrders(originOrders, order, origins, providerFactory) { current, provider ->
            updateBestSell(current, provider, order)
        }
    }

    private suspend fun updateOriginBid(
        originOrders: Set<OriginOrders>,
        order: UnionOrder,
        origins: List<String>,
        providerFactory: BestOrderProviderFactory<*>
    ): Set<OriginOrders> {
        return updateOriginOrders(originOrders, order, origins, providerFactory) { current, provider ->
            updateBestBid(current, provider, order)
        }
    }

    private suspend fun updateOriginOrders(
        originOrders: Set<OriginOrders>,
        order: UnionOrder,
        origins: List<String>,
        providerFactory: BestOrderProviderFactory<*>,
        update: suspend (
            current: OriginOrders,
            bestOrderProvider: BestOrderProvider<*>
        ) -> OriginOrders
    ): Set<OriginOrders> {
        val orderOrigins = order.getOrigins()
        val matchedOrigins = origins.intersect(orderOrigins)
        if (matchedOrigins.isEmpty()) {
            return originOrders // Just to avoid unnecessary garbage production
        }

        val mappedOriginOrders = originOrders.associateBy { it.origin }
        return origins.map { origin ->
            // Here we need to update only origins related to the order
            val current = mappedOriginOrders[origin] ?: OriginOrders(origin)
            if (matchedOrigins.contains(origin)) {
                val provider = providerFactory.create(origin)
                update(current, provider)
            } else {
                // In case if order doesn't relate to the origin, skip it
                current
            }
        }.filterNot { it.isEmpty() }.toSet()
    }

}

