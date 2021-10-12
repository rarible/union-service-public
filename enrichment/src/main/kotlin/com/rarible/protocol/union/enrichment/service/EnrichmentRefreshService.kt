package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.UnionOwnershipDto
import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.enrichment.converter.EnrichedOwnershipConverter
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.event.ItemEventListener
import com.rarible.protocol.union.enrichment.event.ItemEventUpdate
import com.rarible.protocol.union.enrichment.event.OwnershipEventListener
import com.rarible.protocol.union.enrichment.event.OwnershipEventUpdate
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.util.bidCurrencyId
import com.rarible.protocol.union.enrichment.util.sellCurrencyId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EnrichmentRefreshService(
    private val itemService: EnrichmentItemService,
    private val bestOrderService: BestOrderService,
    private val ownershipService: EnrichmentOwnershipService,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val itemEventListeners: List<ItemEventListener>,
    private val ownershipEventListeners: List<OwnershipEventListener>
) {

    private val logger = LoggerFactory.getLogger(EnrichmentRefreshService::class.java)

    suspend fun refreshItemWithOwnerships(itemId: ShortItemId) = coroutineScope {
        TODO()
        logger.info("Starting full refresh of Item [{}] (with ownerships)", itemId)
        val ownerships = ownershipService.fetchAllByItemId(itemId)
        logger.info("Fetched {} Ownerships for Item [{}]", ownerships.size, itemId)
        ownerships
            .map { async { refreshOwnership(it) } }
            .awaitAll()
        refreshItem(itemId)
    }

    suspend fun refreshItem(itemId: ShortItemId) = coroutineScope {
        TODO()
        logger.info("Starting refresh of Item [{}]", itemId)
        val itemDtoDeferred = async { itemService.fetch(itemId) }

        //TODO implement in RPN-1183
        //val bestSellOrdersDeferred = async { enrichmentOrderService.getBestSells(itemId) }
        //val bestBidOrdersDeferred = async { enrichmentOrderService.getBestBids(itemId) }
        val bestSellOrdersDeferred = async { emptyList<OrderDto>() }
        val bestBidOrdersDeferred = async { emptyList<OrderDto>() }

        val bestSellOrdersDto = bestSellOrdersDeferred.await()
        val bestBidOrdersDto = bestBidOrdersDeferred.await()

        val sellStats = ownershipService.getItemSellStats(itemId)

        val itemDto = itemDtoDeferred.await()

        val bestSellOrders = bestSellOrdersDto
            .groupBy { order -> order.sellCurrencyId }
            .mapValues { (_, orders) -> orders.map { order -> ShortOrderConverter.convert(order) } }
            .mapNotNull { (currencyId, orders) ->
                bestOrderService.getBestSellOrder(orders)?.let { best -> currencyId to best }
            }
            .toMap()

        val bestBidOrders = bestBidOrdersDto
            .groupBy { order -> order.bidCurrencyId }
            .mapValues { (_, orders) -> orders.map { order -> ShortOrderConverter.convert(order) } }
            .mapNotNull { (currencyId, orders) ->
                bestOrderService.getBestBidOrder(orders)?.let { best -> currencyId to best }
            }
            .toMap()

        val bestSellOrder = bestOrderService.getBestSellOrderInUsd(bestSellOrders)
        val bestBidOrder = bestOrderService.getBestBidOrderInUsd(bestBidOrders)

        val short = ShortItemConverter.convert(itemDto).copy(
            bestSellOrders = bestSellOrders,
            bestSellOrder = bestSellOrder,
            bestBidOrders = bestBidOrders,
            bestBidOrder = bestBidOrder,
            sellers = sellStats.sellers,
            totalStock = sellStats.totalStock
        )

        if (short.isNotEmpty()) {
            logger.info(
                "Saving refreshed Item [{}] with gathered enrichment data [{}]",
                itemId,
                short
            )
            optimisticLock {
                val currentVersion = itemService.get(itemId)?.version
                itemService.save(short.copy(version = currentVersion))
            }
        } else {
            logger.info("Item [{}] has no enrichment data: {}", itemId, short)
            itemService.delete(itemId)
        }

        val orders = (bestSellOrdersDto + bestBidOrdersDto).associateBy { it.id }

        val dto = EnrichedItemConverter.convert(itemDto, short, orders)
        val event = ItemEventUpdate(dto)

        itemEventListeners.forEach { it.onEvent(event) }
        dto
    }

    private suspend fun refreshOwnership(ownership: UnionOwnershipDto) {
        val short = ShortOwnershipConverter.convert(ownership)

        //val bestSellOrdersDto = enrichmentOrderService.getBestSells(short.id)
        // TODO implement in RPN-1183
        val bestSellOrdersDto = emptyList<OrderDto>()

        val bestSellOrders = bestSellOrdersDto
            .groupBy { order -> order.sellCurrencyId }
            .mapValues { (_, orders) -> orders.map { order -> ShortOrderConverter.convert(order) } }
            .mapNotNull { (currencyId, orders) ->
                bestOrderService.getBestSellOrder(orders)?.let { best -> currencyId to best }
            }
            .toMap()

        val bestSellOrder = bestOrderService.getBestSellOrderInUsd(bestSellOrders)

        val enrichedOwnership = short.copy(
            bestSellOrders = bestSellOrders,
            bestSellOrder = bestSellOrder
        )

        if (enrichedOwnership.isNotEmpty()) {
            logger.info("Updating Ownership [{}] : {}", short.id, enrichedOwnership)
            ownershipService.save(enrichedOwnership)
        } else {
            val result = ownershipService.delete(short.id)
            // Nothing changed for this Ownership, event won't be sent
            if (result == null || result.deletedCount == 0L) {
                return
            }
        }

        val orders = bestSellOrdersDto.associateBy { it.id }

        val dto = EnrichedOwnershipConverter.convert(ownership, short, orders)
        val event = OwnershipEventUpdate(dto)

        ownershipEventListeners.forEach { it.onEvent(event) }
    }
}
