package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.event.OutgoingItemEventListener
import com.rarible.protocol.union.core.event.OutgoingOwnershipEventListener
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.model.getSellerOwnershipId
import com.rarible.protocol.union.core.service.AuctionContractService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.ItemDeleteEventDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.util.bidCurrencyId
import com.rarible.protocol.union.enrichment.util.sellCurrencyId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class EnrichmentRefreshService(
    private val orderServiceRouter: BlockchainRouter<OrderService>,
    private val itemService: EnrichmentItemService,
    private val ownershipService: EnrichmentOwnershipService,
    private val bestOrderService: BestOrderService,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val enrichmentAuctionService: EnrichmentAuctionService,
    private val enrichmentActivityService: EnrichmentActivityService,
    private val enrichmentItemService: EnrichmentItemService,
    private val unionMetaProperties: UnionMetaProperties,
    private val enrichmentOwnershipService: EnrichmentOwnershipService,
    private val itemEventListeners: List<OutgoingItemEventListener>,
    private val ownershipEventListeners: List<OutgoingOwnershipEventListener>,
    private val auctionContractService: AuctionContractService
) {

    private val logger = LoggerFactory.getLogger(EnrichmentRefreshService::class.java)

    suspend fun reconcileItem(itemId: ItemIdDto, full: Boolean) = coroutineScope {
        val shortItemId = ShortItemId(itemId)
        val sellCurrenciesDeferred = async { getSellCurrencies(itemId) }
        val bidCurrenciesDeferred = async { getBidCurrencies(itemId) }

        val auctions = enrichmentAuctionService.findByItem(shortItemId)
        val sellCurrencies = sellCurrenciesDeferred.await()
        val bidCurrencies = bidCurrenciesDeferred.await()

        if (full) {
            reconcileItemOwnerships(itemId, sellCurrencies, auctions)
        }

        reconcileItem(itemId, sellCurrencies, bidCurrencies, auctions)
    }

    private suspend fun reconcileItemOwnerships(
        itemId: ItemIdDto,
        sellCurrencies: List<String>,
        itemAuctions: Collection<AuctionDto>
    ) {
        // Skipping ownerships of Auctions
        val ownerships = ownershipService.fetchAllByItemId(ShortItemId(itemId))
            .filter { !auctionContractService.isAuctionContract(it.id.blockchain, it.id.owner.value) }

        val auctions = itemAuctions.associateBy { it.getSellerOwnershipId() }

        // Checking free or partially auctioned ownerships
        logger.info("Reconciling {} Ownerships for Item [{}]", ownerships.size, itemId)
        coroutineScope {
            ownerships.map { async { reconcileOwnership(it, sellCurrencies, auctions) } }
        }.awaitAll()

        // Checking full auctions and send notifications with disguised ownerships
        val freeOwnershipIds = ownerships.map { it.id }.toHashSet()
        val fullAuctions = auctions.filter { !freeOwnershipIds.contains(it.key) }.values
        fullAuctions.forEach { notifyUpdate(it) }
    }

    suspend fun reconcileOwnership(ownershipId: OwnershipIdDto) = coroutineScope {
        if (auctionContractService.isAuctionContract(ownershipId.blockchain, ownershipId.owner.value)) {
            throw UnionException("Reconciliation for Auction Ownerships is forbidden: $ownershipId")
        }
        // We don't have specific query for ownership, so will use currencies for item
        val itemIdDto = ownershipId.getItemId()
        val shortOwnershipId = ShortOwnershipId(ownershipId)

        val sellCurrencies = async { getSellCurrencies(itemIdDto) }
        val unionOwnershipDeferred = async { ownershipService.fetchOrNull(shortOwnershipId) }
        val auction = enrichmentAuctionService.fetchOwnershipAuction(shortOwnershipId)

        val unionOwnership = unionOwnershipDeferred.await()

        if (unionOwnership != null) {
            // Free or partially auctioned ownership
            val auctions = auction?.let { mapOf(ownershipId to it) } ?: emptyMap()
            reconcileOwnership(unionOwnership, sellCurrencies.await(), auctions)
        } else if (auction != null) {
            // Fully auctioned ownerships - just send disguised ownership event, no enrichment data available here
            notifyUpdate(auction)
        } else {
            // Nothing to reconcile
            null
        }
    }

    private suspend fun reconcileItem(
        itemId: ItemIdDto,
        sellCurrencies: List<String>,
        bidCurrencies: List<String>,
        auctions: Collection<AuctionDto>
    ) = coroutineScope {
        val shortItemId = ShortItemId(itemId)

        logger.info("Starting to reconcile Item [{}]", shortItemId)
        val itemDtoDeferred = async { itemService.fetch(itemId) }
        val sellStatsDeferred = async { ownershipService.getItemSellStats(shortItemId) }

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
                totalStock = sellStats.totalStock,
                auctions = auctions.map { it.id }.toSet()
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

        val event = if (itemDto.deleted) {
            notifyDelete(itemDto.id)
        } else {
            val ordersHint = (bestSellOrdersDto + bestBidOrdersDto).associateBy { it.id }
            val auctionsHint = auctions.associateBy { it.id }
            val enriched = enrichmentItemService.enrichItem(
                shortItem = updatedItem,
                item = itemDto,
                orders = ordersHint,
                auctions = auctionsHint
            )
            notifyUpdate(enriched)
        }
        event
    }

    private suspend fun reconcileOwnership(
        ownership: UnionOwnership,
        currencies: List<String>,
        auctions: Map<OwnershipIdDto, AuctionDto>
    ) = coroutineScope {
        val shortOwnershipId = ShortOwnershipId(ownership.id)

        val ownershipSource = async { enrichmentActivityService.getOwnershipSource(shortOwnershipId.toDto()) }

        val bestSellOrdersDto = currencies.map { currencyId ->
            async { enrichmentOrderService.getBestSell(shortOwnershipId, currencyId) }
        }.awaitAll().filterNotNull()

        val bestSellOrders = bestSellOrdersDto.associateBy { it.sellCurrencyId }
            .mapValues { ShortOrderConverter.convert(it.value) }

        val updatedOwnership = optimisticLock {
            val shortOwnership = enrichmentOwnershipService.getOrEmpty(shortOwnershipId).copy(
                bestSellOrders = bestSellOrders,
                bestSellOrder = bestOrderService.getBestSellOrderInUsd(bestSellOrders),
                source = ownershipSource.await()
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

        notifyUpdate(updatedOwnership, ownership, ordersHint, auctions)
    }

    private suspend fun notifyUpdate(
        short: ShortOwnership,
        ownership: UnionOwnership,
        orders: Map<OrderIdDto, OrderDto> = emptyMap(),
        auctions: Map<OwnershipIdDto, AuctionDto> = emptyMap()
    ): OwnershipEventDto {
        val enriched = ownershipService.enrichOwnership(short, ownership, orders)
        val dto = ownershipService.mergeWithAuction(enriched, auctions[enriched.id])
        return notifyUpdate(dto)
    }

    private suspend fun notifyUpdate(
        auction: AuctionDto
    ): OwnershipEventDto? {
        val dto = enrichmentOwnershipService.disguiseAuctionWithEnrichment(auction)
        return dto?.let { notifyUpdate(dto) }
    }

    private suspend fun notifyUpdate(dto: OwnershipDto): OwnershipEventDto {
        val event = OwnershipUpdateEventDto(
            ownershipId = dto.id,
            ownership = dto,
            eventId = UUID.randomUUID().toString()
        )
        ownershipEventListeners.forEach { it.onEvent(event) }
        return event
    }

    private suspend fun notifyDelete(itemId: ItemIdDto): ItemEventDto {
        val event = ItemDeleteEventDto(
            itemId = itemId,
            eventId = UUID.randomUUID().toString()
        )
        itemEventListeners.forEach { it.onEvent(event) }
        return event
    }

    private suspend fun notifyUpdate(itemDto: ItemDto): ItemEventDto {
        val event = ItemUpdateEventDto(
            itemId = itemDto.id,
            item = itemDto,
            eventId = UUID.randomUUID().toString()
        )
        itemEventListeners.forEach { it.onEvent(event) }
        return event
    }

    private suspend fun getBidCurrencies(itemId: ItemIdDto): List<String> {
        val result = orderServiceRouter.getService(itemId.blockchain)
            .getBidCurrencies(itemId.value)

        logger.info("Found Bid currencies for Item [{}] : {}", itemId.fullId(), result)
        return result.map { it.ext.currencyAddress() }
    }

    private suspend fun getSellCurrencies(itemId: ItemIdDto): List<String> {
        val result = orderServiceRouter.getService(itemId.blockchain)
            .getSellCurrencies(itemId.value)

        logger.info("Found Sell currencies for Item [{}] : {}", itemId.fullId(), result)
        return result.map { it.ext.currencyAddress() }

    }
}
