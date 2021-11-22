package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.event.OutgoingItemEventListener
import com.rarible.protocol.union.core.event.OutgoingOwnershipEventListener
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.enrichment.converter.EnrichedOwnershipConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.util.bidCurrencyId
import com.rarible.protocol.union.enrichment.util.sellCurrencyId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class EnrichmentRefreshService(
    private val orderServiceRouter: BlockchainRouter<OrderService>,
    private val itemService: EnrichmentItemService,
    private val ownershipService: EnrichmentOwnershipService,
    private val bestOrderService: BestOrderService,
    private val enrichmentMetaService: EnrichmentMetaService,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val enrichmentAuctionService: EnrichmentAuctionService,
    private val enrichmentItemService: EnrichmentItemService,
    private val enrichmentOwnershipService: EnrichmentOwnershipService,
    private val itemEventListeners: List<OutgoingItemEventListener>,
    private val ownershipEventListeners: List<OutgoingOwnershipEventListener>
) {

    private val logger = LoggerFactory.getLogger(EnrichmentRefreshService::class.java)

    suspend fun refreshItem(itemId: ItemIdDto): ItemDto {
        val shortItemId = ShortItemId(itemId)
        val unionItem = enrichmentItemService.fetch(shortItemId)

        return optimisticLock {
            val shortItem = itemService.getOrEmpty(shortItemId)
            val auctions = enrichmentAuctionService.findByItem(shortItem).map { it.id }.toSet()
            val updatedItem = bestOrderService.updateBestOrders(shortItem)
                .copy(auctions = auctions)
            if (updatedItem != shortItem) {
                enrichmentItemService.save(updatedItem)
            }
            notifyUpdate(updatedItem, unionItem)
        }
    }

    suspend fun refreshOwnership(ownershipId: OwnershipIdDto): OwnershipDto {
        val shortOwnershipId = ShortOwnershipId(ownershipId)
        val unionOwnership = enrichmentOwnershipService.fetch(shortOwnershipId)

        return optimisticLock {
            val shortOwnership = ownershipService.getOrEmpty(shortOwnershipId)
            val updatedOwnership = bestOrderService.updateBestSellOrder(shortOwnership)
            if (updatedOwnership != shortOwnership) {
                enrichmentOwnershipService.save(updatedOwnership)
            }
            notifyUpdate(updatedOwnership, unionOwnership)
        }
    }

    suspend fun reconcileItem(itemId: ItemIdDto, full: Boolean): ItemDto {
        val sellCurrencies = getSellCurrencies(itemId)
        val bidCurrencies = getBidCurrencies(itemId)

        if (full) {
            logger.info("Starting reconciliation of Item [{}] (with ownerships)", itemId)
            val ownerships = ownershipService.fetchAllByItemId(ShortItemId(itemId))

            logger.info("Fetched {} Ownerships for Item [{}]", ownerships.size, itemId)
            coroutineScope {
                ownerships.map { async { reconcileOwnership(it, sellCurrencies) } }
            }.awaitAll()
        }
        return reconcileItem(itemId, sellCurrencies, bidCurrencies)
    }

    suspend fun reconcileOwnership(ownershipId: OwnershipIdDto) = coroutineScope {
        // We don't have specific query for ownership, so will use currencies for item
        val itemIdDto = ItemIdDto(ownershipId.blockchain, ownershipId.contract, ownershipId.tokenId)

        val sellCurrencies = async { getSellCurrencies(itemIdDto) }
        val unionOwnership = async { ownershipService.fetch(ShortOwnershipId(ownershipId)) }

        reconcileOwnership(unionOwnership.await(), sellCurrencies.await())
    }

    private suspend fun reconcileItem(
        itemId: ItemIdDto,
        sellCurrencies: List<String>,
        bidCurrencies: List<String>
    ) = coroutineScope {
        val shortItemId = ShortItemId(itemId)

        logger.info("Starting refresh of Item [{}]", shortItemId)
        val itemDtoDeferred = async { itemService.fetch(shortItemId) }
        val sellStatsDeferred = async { ownershipService.getItemSellStats(shortItemId) }
        val metaDeferred = async {
            val meta = itemDtoDeferred.await().meta
            meta?.let { enrichmentMetaService.enrichMeta(meta, shortItemId) }
        }

        // Looking for best sell orders
        val bestSellOrdersDto = sellCurrencies.map { currencyId ->
            async { enrichmentOrderService.getBestSell(shortItemId, currencyId) }
        }.awaitAll().filterNotNull()

        val bestSellOrders = bestSellOrdersDto.associateBy { it.sellCurrencyId }
            .mapValues { ShortOrderConverter.convert(it.value) }

        // Looking for best bid orders
        val bestBidOrdersDto = bidCurrencies.map { currencyId ->
            async { enrichmentOrderService.getBestBid(shortItemId, currencyId) }
        }.awaitAll().filterNotNull()

        val bestBidOrders = bestBidOrdersDto.associateBy { it.bidCurrencyId }
            .mapValues { ShortOrderConverter.convert(it.value) }

        // Waiting other operations completed
        val sellStats = sellStatsDeferred.await()
        val itemDto = itemDtoDeferred.await()

        val updatedItem = optimisticLock {
            val shortItem = itemService.getOrEmpty(shortItemId).copy(
                bestSellOrders = bestSellOrders,
                bestSellOrder = bestOrderService.getBestSellOrderInUsd(bestSellOrders),
                bestBidOrders = bestBidOrders,
                bestBidOrder = bestOrderService.getBestBidOrderInUsd(bestBidOrders),
                sellers = sellStats.sellers,
                totalStock = sellStats.totalStock
            )

            if (shortItem.isNotEmpty()) {
                logger.info("Saving refreshed Item [{}] with gathered enrichment data [{}]", itemId, shortItem)
                itemService.save(shortItem)
            } else {
                logger.info("Item [{}] has no enrichment data, will be deleted", itemId)
                itemService.delete(shortItemId)
            }
            shortItem
        }

        val ordersHint = (bestSellOrdersDto + bestBidOrdersDto).associateBy { it.id }

        val dto = EnrichedItemConverter.convert(itemDto, updatedItem, metaDeferred.await(), ordersHint)
        val event = ItemUpdateEventDto(
            itemId = dto.id,
            item = dto,
            eventId = UUID.randomUUID().toString()
        )
        itemEventListeners.forEach { it.onEvent(event) }

        dto
    }

    private suspend fun reconcileOwnership(ownership: UnionOwnership, currencies: List<String>) = coroutineScope {
        val shortOwnershipId = ShortOwnershipId(ownership.id)

        val bestSellOrdersDto = currencies.map { currencyId ->
            async { enrichmentOrderService.getBestSell(shortOwnershipId, currencyId) }
        }.awaitAll().filterNotNull()

        val bestSellOrders = bestSellOrdersDto.associateBy { it.sellCurrencyId }
            .mapValues { ShortOrderConverter.convert(it.value) }

        val updatedOwnership = optimisticLock {
            val shortOwnership = enrichmentOwnershipService.getOrEmpty(shortOwnershipId).copy(
                bestSellOrders = bestSellOrders,
                bestSellOrder = bestOrderService.getBestSellOrderInUsd(bestSellOrders)
            )

            if (shortOwnership.isNotEmpty()) {
                logger.info("Updating Ownership [{}] : {}", shortOwnershipId, shortOwnership)
                ownershipService.save(shortOwnership)
            } else {
                logger.info("Ownership [{}] has no enrichment data, will be deleted", shortOwnershipId)
                ownershipService.delete(shortOwnershipId)
            }
            shortOwnership
        }

        val ordersHint = bestSellOrdersDto.associateBy { it.id }
        val dto = EnrichedOwnershipConverter.convert(ownership, updatedOwnership, ordersHint)
        val event = OwnershipUpdateEventDto(
            ownershipId = dto.id,
            ownership = dto,
            eventId = UUID.randomUUID().toString()
        )

        ownershipEventListeners.forEach { it.onEvent(event) }
        dto
    }

    private suspend fun notifyUpdate(
        short: ShortItem,
        item: UnionItem
    ): ItemDto {
        val dto = itemService.enrichItem(short, item, emptyMap(), emptyMap())
        val event = ItemUpdateEventDto(
            itemId = dto.id,
            item = dto,
            eventId = UUID.randomUUID().toString()
        )
        itemEventListeners.forEach { it.onEvent(event) }
        return dto
    }

    private suspend fun notifyUpdate(
        short: ShortOwnership,
        ownership: UnionOwnership
    ): OwnershipDto {
        val dto = ownershipService.enrichOwnership(short, ownership, emptyMap())
        val event = OwnershipUpdateEventDto(
            ownershipId = dto.id,
            ownership = dto,
            eventId = UUID.randomUUID().toString()
        )
        ownershipEventListeners.forEach { it.onEvent(event) }
        return dto
    }

    private suspend fun getBidCurrencies(itemId: ItemIdDto): List<String> {
        val result = orderServiceRouter.getService(itemId.blockchain)
            .getBidCurrencies(itemId.contract, itemId.tokenId.toString())

        logger.info("Found Bid currencies for Item [{}] : {}", itemId.fullId(), result)
        return result.map { it.ext.contract }
    }

    private suspend fun getSellCurrencies(itemId: ItemIdDto): List<String> {
        val result = orderServiceRouter.getService(itemId.blockchain)
            .getSellCurrencies(itemId.contract, itemId.tokenId.toString())

        logger.info("Found Sell currencies for Item [{}] : {}", itemId.fullId(), result)
        return result.map { it.ext.contract }

    }
}
