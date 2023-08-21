package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.event.OutgoingEventListener
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.model.getSellerOwnershipId
import com.rarible.protocol.union.core.service.AuctionContractService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.OriginService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.dto.ItemDeleteEventDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.enrichment.converter.EnrichmentCollectionConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.evaluator.BestOrderProviderFactory
import com.rarible.protocol.union.enrichment.evaluator.BestSellOrderComparator
import com.rarible.protocol.union.enrichment.evaluator.CollectionBestBidOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.CollectionBestSellOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.ItemBestBidOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.ItemBestSellOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.OwnershipBestBidOrderProvider
import com.rarible.protocol.union.enrichment.evaluator.OwnershipBestSellOrderProvider
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.OriginOrders
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.model.ShortPoolOrder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EnrichmentRefreshService(
    private val orderServiceRouter: BlockchainRouter<OrderService>,
    private val itemService: EnrichmentItemService,
    private val ownershipService: EnrichmentOwnershipService,
    private val bestOrderService: BestOrderService,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val enrichmentAuctionService: EnrichmentAuctionService,
    private val enrichmentActivityService: EnrichmentActivityService,
    private val enrichmentCollectionService: EnrichmentCollectionService,
    private val enrichmentItemService: EnrichmentItemService,
    private val enrichmentOwnershipService: EnrichmentOwnershipService,
    private val collectionEventListeners: List<OutgoingEventListener<CollectionEventDto>>,
    private val itemEventListeners: List<OutgoingEventListener<ItemEventDto>>,
    private val ownershipEventListeners: List<OutgoingEventListener<OwnershipEventDto>>,
    private val auctionContractService: AuctionContractService,
    private val originService: OriginService,
    private val ff: FeatureFlagsProperties
) {

    private val logger = LoggerFactory.getLogger(EnrichmentRefreshService::class.java)

    suspend fun reconcileCollection(collectionId: CollectionIdDto) = coroutineScope {
        val enrichmentCollectionId = EnrichmentCollectionId(collectionId)
        val sellCurrenciesDeferred = async { getSellCurrencies(collectionId) }
        val bidCurrenciesDeferred = async { getBidCurrencies(collectionId) }

        val sellCurrencies = sellCurrenciesDeferred.await()
        val bidCurrencies = bidCurrenciesDeferred.await()

        reconcileCollection(enrichmentCollectionId, sellCurrencies, bidCurrencies)
    }

    suspend fun reconcileItem(itemId: ItemIdDto, full: Boolean) = coroutineScope {
        val shortItemId = ShortItemId(itemId)
        val sellCurrenciesDeferred = async { getSellCurrencies(itemId) }
        val bidCurrenciesDeferred = async { getBidCurrencies(itemId) }

        val auctions = enrichmentAuctionService.findByItem(shortItemId)
        val sellCurrencies = sellCurrenciesDeferred.await()
        val bidCurrencies = bidCurrenciesDeferred.await()
        val ammOrders = getAmmOrders(itemId)

        if (full) {
            reconcileItemOwnerships(itemId, sellCurrencies, auctions, ammOrders)
        }

        reconcileItem(itemId, sellCurrencies, bidCurrencies, auctions, ammOrders)
    }

    private suspend fun reconcileItemOwnerships(
        itemId: ItemIdDto,
        sellCurrencies: List<String>,
        itemAuctions: Collection<AuctionDto>,
        ammOrders: List<UnionOrder>
    ) {
        // Skipping ownerships of Auctions
        val shortItemId = ShortItemId(itemId)
        val ownerships = ownershipService.fetchAllByItemId(shortItemId)
            .filter { !auctionContractService.isAuctionContract(it.id.blockchain, it.id.owner.value) }

        val auctions = itemAuctions.associateBy { it.getSellerOwnershipId() }
        val origins = enrichmentItemService.getItemOrigins(shortItemId)
        val ammOrdersByUser = ammOrders.associateBy { it.maker }

        // Checking free or partially auctioned ownerships
        logger.info("Reconciling {} Ownerships for Item [{}]", ownerships.size, itemId)
        coroutineScope {
            ownerships.map {
                async {
                    reconcileOwnership(it, sellCurrencies, auctions, ammOrdersByUser[it.id.owner], origins)
                }
            }
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
        val itemId = ownershipId.getItemId()
        val shortOwnershipId = ShortOwnershipId(ownershipId)

        val sellCurrencies = async { getSellCurrencies(itemId) }
        val unionOwnershipDeferred = async { ownershipService.fetchOrNull(shortOwnershipId) }
        val auction = enrichmentAuctionService.fetchOwnershipAuction(shortOwnershipId)

        val unionOwnership = unionOwnershipDeferred.await()

        if (unionOwnership != null) {
            // Free or partially auctioned ownership
            val ammOrder = getAmmOrders(itemId).firstOrNull { it.maker == ownershipId.owner }
            val auctions = auction?.let { mapOf(ownershipId to it) } ?: emptyMap()
            val origins = enrichmentItemService.getItemOrigins(shortOwnershipId.getItemId())
            reconcileOwnership(unionOwnership, sellCurrencies.await(), auctions, ammOrder, origins)
        } else if (auction != null) {
            // Fully auctioned ownerships - just send disguised ownership event, no enrichment data available here
            notifyUpdate(auction)
        } else {
            // Nothing to reconcile
            null
        }
    }

    private suspend fun reconcileCollection(
        enrichmentCollectionId: EnrichmentCollectionId,
        sellCurrencies: List<String>,
        bidCurrencies: List<String>
    ) = coroutineScope {
        logger.info("Starting to reconcile Collection [{}]", enrichmentCollectionId)
        val current = enrichmentCollectionService.get(enrichmentCollectionId)
        if (current == null) {
            logger.info("Didn't find collection in union database during reconciliation: $enrichmentCollectionId")
        }
        val artificial = current != null && current.structure != UnionCollection.Structure.REGULAR

        val unionCollection = if (artificial) {
            null
        } else {
            enrichmentCollectionService.fetch(enrichmentCollectionId)
        }

        val bestSellProviderFactory =
            CollectionBestSellOrderProvider.Factory(enrichmentCollectionId, enrichmentOrderService)

        val bestBidProviderFactory =
            CollectionBestBidOrderProvider.Factory(enrichmentCollectionId, enrichmentOrderService)

        val origins = originService.getOrigins(enrichmentCollectionId.toDto())
        val bestOrders = getOriginBestOrders(
            origins, sellCurrencies, bidCurrencies, bestSellProviderFactory, bestBidProviderFactory, emptyList()
        )

        val updatedCollection = optimisticLock {

            val exist = if (artificial) {
                enrichmentCollectionService.get(enrichmentCollectionId)!!
            } else {
                enrichmentCollectionService.get(enrichmentCollectionId)?.withData(unionCollection!!)
                    ?: EnrichmentCollectionConverter.convert(unionCollection!!)
            }

            val enrichmentCollection = exist.copy(
                bestSellOrders = bestOrders.global.bestSellOrders,
                bestSellOrder = bestOrders.global.bestSellOrder,
                bestBidOrders = bestOrders.global.bestBidOrders,
                bestBidOrder = bestOrders.global.bestBidOrder,
                originOrders = bestOrders.originOrders
            )

            logger.info(
                "Saving refreshed Collection [{}] with gathered enrichment data [{}]", enrichmentCollectionId,
                enrichmentCollection
            )
            enrichmentCollectionService.save(enrichmentCollection)
        }

        val ordersHint = bestOrders.all
        val enriched = enrichmentCollectionService.enrichCollection(
            enrichmentCollection = updatedCollection,
            orders = ordersHint,
            metaPipeline = CollectionMetaPipeline.REFRESH
        )
        notifyUpdate(enriched)
    }

    private suspend fun reconcileItem(
        itemId: ItemIdDto,
        sellCurrencies: List<String>,
        bidCurrencies: List<String>,
        auctions: Collection<AuctionDto>,
        ammOrders: List<UnionOrder>
    ) = coroutineScope {
        val shortItemId = ShortItemId(itemId)

        logger.info("Starting to reconcile Item [{}]", shortItemId)
        val lastSaleDeferred = async {
            if (ff.enableItemLastSaleEnrichment) enrichmentActivityService.getItemLastSale(itemId) else null
        }
        val itemDtoDeferred = async { itemService.fetch(shortItemId) }
        val sellStatsDeferred = async { ownershipService.getItemSellStats(shortItemId) }
        val poolSellOrders = ammOrders.map { ShortPoolOrder(it.sellCurrencyId(), ShortOrderConverter.convert(it)) }

        // Reset pool sell orders before recalculations in order to avoid unnecessary getOrder() calls
        val shortItem = itemService.getOrEmpty(shortItemId).copy(poolSellOrders = emptyList())

        val bestSellProviderFactory = ItemBestSellOrderProvider.Factory(
            shortItem, enrichmentOrderService, ff.enablePoolOrders
        )
        val bestBidProviderFactory = ItemBestBidOrderProvider.Factory(shortItem, enrichmentOrderService)

        val origins = enrichmentItemService.getItemOrigins(shortItemId)
        val bestOrders = getOriginBestOrders(
            origins, sellCurrencies, bidCurrencies, bestSellProviderFactory, bestBidProviderFactory, ammOrders
        )

        // Waiting other operations completed
        val sellStats = sellStatsDeferred.await()
        val itemDto = itemDtoDeferred.await()

        val updatedItem = optimisticLock {
            val currentItem = itemService.getOrEmpty(shortItemId).copy(
                bestSellOrders = bestOrders.global.bestSellOrders,
                bestSellOrder = bestOrders.global.bestSellOrder,
                bestBidOrders = bestOrders.global.bestBidOrders,
                bestBidOrder = bestOrders.global.bestBidOrder,
                originOrders = bestOrders.originOrders,
                sellers = sellStats.sellers,
                totalStock = sellStats.totalStock,
                auctions = auctions.map { it.id }.toSet(),
                lastSale = lastSaleDeferred.await(),
                poolSellOrders = poolSellOrders
            )

            logger.info("Saving refreshed Item [{}] with gathered enrichment data [{}]", itemId, currentItem)
            itemService.save(currentItem)

            currentItem
        }

        val event = if (itemDto.deleted) {
            notifyDelete(itemDto.id)
        } else {
            val ordersHint = bestOrders.all
            val auctionsHint = auctions.associateBy { it.id }
            val enriched = enrichmentItemService.enrichItem(
                shortItem = updatedItem,
                item = itemDto,
                orders = ordersHint,
                auctions = auctionsHint,
                metaPipeline = ItemMetaPipeline.REFRESH
            )
            notifyUpdate(enriched)
        }
        event
    }

    private suspend fun reconcileOwnership(
        ownership: UnionOwnership,
        currencies: List<String>,
        auctions: Map<OwnershipIdDto, AuctionDto>,
        ammOrders: UnionOrder?,
        origins: List<String>
    ) = coroutineScope {
        val shortOwnershipId = ShortOwnershipId(ownership.id)

        val ownershipSource = async {
            if (ff.enableOwnershipSourceEnrichment) enrichmentActivityService.getOwnershipSource(ownership.id) else null
        }

        val bestSellProviderFactory = OwnershipBestSellOrderProvider.Factory(shortOwnershipId, enrichmentOrderService)
        val bestBidProviderFactory = OwnershipBestBidOrderProvider.Factory(shortOwnershipId, enrichmentOrderService)

        val bestOrders = getOriginBestOrders(
            origins, currencies, emptyList(), bestSellProviderFactory, bestBidProviderFactory, listOfNotNull(ammOrders)
        )

        val updatedOwnership = optimisticLock {
            val shortOwnership = enrichmentOwnershipService.getOrEmpty(shortOwnershipId).copy(
                bestSellOrders = bestOrders.global.bestSellOrders,
                bestSellOrder = bestOrders.global.bestSellOrder,
                originOrders = bestOrders.originOrders,
                source = ownershipSource.await()
            )

            logger.info("Updating Ownership [{}] : {}", shortOwnershipId, shortOwnership)
            ownershipService.save(shortOwnership)
        }

        val ordersHint = bestOrders.all

        notifyUpdate(updatedOwnership, ownership, ordersHint, auctions)
    }

    private suspend fun notifyUpdate(
        short: ShortOwnership,
        ownership: UnionOwnership,
        orders: Map<OrderIdDto, UnionOrder> = emptyMap(),
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

    private suspend fun notifyUpdate(collectionDto: CollectionDto): CollectionEventDto {
        val event = CollectionUpdateEventDto(
            collectionId = collectionDto.id,
            collection = collectionDto,
            eventId = UUID.randomUUID().toString()
        )
        collectionEventListeners.forEach { it.onEvent(event) }
        return event
    }

    private suspend fun getBidCurrencies(itemId: ItemIdDto): List<String> {
        val result = orderServiceRouter.getService(itemId.blockchain)
            .getBidCurrencies(itemId.value, listOf(OrderStatusDto.ACTIVE))

        logger.info("Found Bid currencies for Item [{}] : {}", itemId.fullId(), result)
        return result.map { it.currencyId()!! }
    }

    private suspend fun getSellCurrencies(itemId: ItemIdDto): List<String> {
        val result = orderServiceRouter.getService(itemId.blockchain)
            .getSellCurrencies(itemId.value, listOf(OrderStatusDto.ACTIVE))

        logger.info("Found Sell currencies for Item [{}] : {}", itemId.fullId(), result)
        return result.map { it.currencyId()!! }
    }

    private suspend fun getBidCurrencies(collectionId: CollectionIdDto): List<String> {
        val result = orderServiceRouter.getService(collectionId.blockchain)
            .getBidCurrenciesByCollection(collectionId.value, listOf(OrderStatusDto.ACTIVE))

        logger.info("Found Bid currencies for Collection [{}] : {}", collectionId.fullId(), result)
        return result.map { it.currencyId()!! }
    }

    private suspend fun getSellCurrencies(collectionId: CollectionIdDto): List<String> {
        val result = orderServiceRouter.getService(collectionId.blockchain)
            .getSellCurrenciesByCollection(collectionId.value, listOf(OrderStatusDto.ACTIVE))

        logger.info("Found Sell currencies for Collection [{}] : {}", collectionId.fullId(), result)
        return result.map { it.currencyId()!! }
    }

    private suspend fun getAmmOrders(itemId: ItemIdDto): List<UnionOrder> {
        if (!ff.enablePoolOrders) {
            return emptyList()
        }
        val result = orderServiceRouter.fetchAllBySlices(itemId.blockchain) { service, continuation ->
            service.getAmmOrdersByItem(itemId.value, continuation, 200)
        }
        logger.info("Found ${result.size} AMM orders for the Item: $itemId")
        return result
    }

    private suspend fun getOriginBestOrders(
        origins: List<String>,
        sellCurrencies: List<String>,
        bidCurrencies: List<String>,
        bestSellProviderFactory: BestOrderProviderFactory<*>,
        bestBidProviderFactory: BestOrderProviderFactory<*>,
        poolOrders: List<UnionOrder>
    ) = coroutineScope {
        val poolOrdersByCurrency = poolOrders.groupBy { it.sellCurrencyId() }
        val originsDeferred = origins.map { origin ->
            async {
                getBestOrders(
                    origin,
                    sellCurrencies,
                    bidCurrencies,
                    bestSellProviderFactory,
                    bestBidProviderFactory,
                    emptyMap() // Origin orders should be not affected by pool orders
                )
            }
        }
        val global = getBestOrders(
            null,
            sellCurrencies,
            bidCurrencies,
            bestSellProviderFactory,
            bestBidProviderFactory,
            poolOrdersByCurrency
        )

        val byOrigin = originsDeferred.awaitAll()

        val all = HashMap(global.all)
        byOrigin.forEach { all.putAll(it.all) }

        OriginBestOrders(
            all,
            global.orders,
            byOrigin.map { it.orders }.toSet()
        )
    }

    private suspend fun getBestOrders(
        origin: String?,
        sellCurrencies: List<String>,
        bidCurrencies: List<String>,
        bestSellProviderFactory: BestOrderProviderFactory<*>,
        bestBidProviderFactory: BestOrderProviderFactory<*>,
        poolOrders: Map<String, List<UnionOrder>>
    ) = coroutineScope {
        // Looking for best sell orders
        val bestSellOrdersDtoDeferred = sellCurrencies.map { currencyId ->
            async {
                val bestSellByCurrency = bestSellProviderFactory.create(origin).fetch(currencyId)
                chooseBestOrder(currencyId, bestSellByCurrency, poolOrders)
            }
        }

        // Looking for best bid orders
        val bestBidOrdersDtoDeferred = bidCurrencies.map { currencyId ->
            async { bestBidProviderFactory.create(origin).fetch(currencyId) }
        }
        val bestSellOrdersDto = bestSellOrdersDtoDeferred.awaitAll().filterNotNull()
        val bestBidOrdersDto = bestBidOrdersDtoDeferred.awaitAll().filterNotNull()

        val bestSellOrders = bestSellOrdersDto.associateBy { it.sellCurrencyId() }
            .mapValues { ShortOrderConverter.convert(it.value) }

        val bestBidOrders = bestBidOrdersDto.associateBy { it.bidCurrencyId() }
            .mapValues { ShortOrderConverter.convert(it.value) }

        val all = (bestSellOrdersDto + bestBidOrdersDto).associateBy { it.id }

        BestOrders(
            all,
            OriginOrders(
                origin ?: "global",
                bestOrderService.getBestSellOrderInUsd(bestSellOrders),
                bestSellOrders,
                bestOrderService.getBestBidOrderInUsd(bestBidOrders),
                bestBidOrders
            )
        )
    }

    private fun chooseBestOrder(
        currencyId: String,
        directOrder: UnionOrder?,
        poolOrders: Map<String, List<UnionOrder>>
    ): UnionOrder? {
        val poolOrdersByCurrency = poolOrders[currencyId] ?: emptyList()
        val allOrders = directOrder?.let { poolOrdersByCurrency + it } ?: poolOrdersByCurrency
        val mappedAllOrders = allOrders.associateBy { it.id.value }
        val best = allOrders.map { ShortOrderConverter.convert(it) }
            .reduceOrNull(BestSellOrderComparator::compare)
        return best?.let { mappedAllOrders[best.id] }
    }

    data class OriginBestOrders(
        val all: Map<OrderIdDto, UnionOrder>,
        val global: OriginOrders,
        val originOrders: Set<OriginOrders>
    )

    data class BestOrders(
        val all: Map<OrderIdDto, UnionOrder>,
        val orders: OriginOrders
    )
}
