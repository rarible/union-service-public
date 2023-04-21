package com.rarible.protocol.union.enrichment.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.core.common.flatMapAsync
import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.converter.OrderDtoConverter
import com.rarible.protocol.union.enrichment.converter.data.EnrichmentOrderData
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOrder
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.util.spent
import com.rarible.protocol.union.enrichment.validator.BestOrderValidator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EnrichmentOrderService(
    private val orderServiceRouter: BlockchainRouter<OrderService>,
    private val customCollectionResolver: CustomCollectionResolver
) {

    private val logger = LoggerFactory.getLogger(EnrichmentOrderService::class.java)

    suspend fun enrich(order: UnionOrder): OrderDto {
        // We expect here only one of them != null
        val itemId = order.itemId()
        val collectionId = order.assetCollectionId()

        val customCollection = when {
            itemId != null -> customCollectionResolver.resolveCustomCollection(itemId)
            collectionId != null -> customCollectionResolver.resolveCustomCollection(collectionId)
            else -> null
        }

        val data = EnrichmentOrderData(customCollection)
        return OrderDtoConverter.convert(order, data)
    }

    suspend fun enrich(orders: List<UnionOrder>): List<OrderDto> {
        return orders.map { enrich(it) }
    }

    suspend fun enrich(orders: Map<OrderIdDto, UnionOrder>): Map<OrderIdDto, OrderDto> {
        return orders.mapValues { enrich(it.value) }
    }

    suspend fun getById(id: OrderIdDto): UnionOrder? {
        return try {
            orderServiceRouter.getService(id.blockchain).getOrderById(id.value)
        } catch (e: WebClientResponseProxyException) {
            logger.warn("Unable to retrieve original Order [{}] from indexer: {}, response: {}", id, e.message, e.data)
            null
        }
    }

    suspend fun getByIds(ids: List<OrderIdDto>): List<UnionOrder> {
        return ids
            .groupBy { it.blockchain }
            .flatMapAsync { (blockchain, ids) ->
                val nativeIds = ids.map { it.value }
                orderServiceRouter.getService(blockchain).getOrdersByIds(nativeIds)
            }
    }

    suspend fun fetchMissingOrders(
        existing: List<ShortOrder?>,
        orders: Map<OrderIdDto, UnionOrder>
    ): Map<OrderIdDto, UnionOrder> {
        val notNull = existing.filterNotNull()
            .map { it.dtoId }
            .filterNot { orders.containsKey(it) }

        // Nothing to download
        if (notNull.isEmpty()) {
            return orders
        }

        val fetched = getByIds(notNull)

        // Merging with already known orders
        return fetched.associateBy { it.id } + orders
    }

    suspend fun getBestSell(id: ShortItemId, currencyId: String, origin: String?): UnionOrder? {
        val result = withPreferredRariblePlatform(
            id, OrderType.SELL, OrderFilters.ITEM
        ) { platform, continuation, size ->
            orderServiceRouter.getService(id.blockchain).getSellOrdersByItem(
                platform,
                id.toDto().value,
                null,
                origin,
                listOf(OrderStatusDto.ACTIVE),
                currencyId,
                continuation,
                size
            )
        }
        return result
    }

    suspend fun getBestSell(id: ShortOwnershipId, currencyId: String, origin: String?): UnionOrder? {
        val now = nowMillis()
        val result = withPreferredRariblePlatform(
            id, OrderType.SELL, OrderFilters.ITEM
        ) { platform, continuation, size ->
            orderServiceRouter.getService(id.blockchain).getSellOrdersByItem(
                platform,
                id.toDto().itemIdValue,
                id.owner,
                origin,
                listOf(OrderStatusDto.ACTIVE),
                currencyId,
                continuation,
                size
            )
        }
        logger.info(
            "Fetched best sell Order for Ownership [{}]: [{}], status = {} ({}ms)",
            id.toDto().fullId(), result?.id, result?.status, spent(now)
        )
        return result
    }

    suspend fun getBestSell(collectionId: EnrichmentCollectionId, currencyId: String, origin: String?): UnionOrder? {
        val now = nowMillis()
        val result = withPreferredRariblePlatform(
            collectionId, OrderType.SELL, OrderFilters.COLLECTION
        ) { platform, continuation, size ->
            orderServiceRouter.getService(collectionId.blockchain).getOrderFloorSellsByCollection(
                platform,
                collectionId.toDto().value,
                origin,
                listOf(OrderStatusDto.ACTIVE),
                currencyId,
                continuation,
                size
            )
        }
        logger.info(
            "Fetched best sell Order for Item [{}]: [{}], status = {} ({}ms)",
            collectionId.toDto().fullId(), result?.id, result?.status, spent(now)
        )
        return result
    }

    suspend fun getBestBid(id: ShortItemId, currencyId: String, origin: String?): UnionOrder? {
        val now = nowMillis()
        val result = withPreferredRariblePlatform(
            id, OrderType.BID, OrderFilters.ITEM
        ) { platform, continuation, size ->
            orderServiceRouter.getService(id.blockchain).getOrderBidsByItem(
                platform,
                id.toDto().value,
                null,
                origin,
                listOf(OrderStatusDto.ACTIVE),
                null,
                null,
                currencyId,
                continuation,
                size
            )
        }
        logger.info(
            "Fetching best bid Order for Item [{}]: [{}], status = {} ({}ms)",
            id.toDto().fullId(), result?.id, result?.status, spent(now)
        )
        return result
    }

    suspend fun getBestBid(id: EnrichmentCollectionId, currencyId: String, origin: String?): UnionOrder? {
        val now = nowMillis()
        val result = withPreferredRariblePlatform(
            id, OrderType.BID, OrderFilters.COLLECTION
        ) { platform, continuation, size ->
            orderServiceRouter.getService(id.blockchain).getOrderFloorBidsByCollection(
                platform,
                id.toDto().value,
                origin,
                listOf(OrderStatusDto.ACTIVE),
                null,
                null,
                currencyId,
                continuation,
                size
            )
        }
        logger.info(
            "Fetching best bid Order for Item [{}]: [{}], status = {} ({}ms)",
            id.toDto().fullId(), result?.id, result?.status, spent(now)
        )
        return result
    }

    private suspend fun withPreferredRariblePlatform(
        id: Any,
        orderType: OrderType,
        filter: OrderFilters = OrderFilters.ALL,
        clientCall: suspend (platform: PlatformDto?, continuation: String?, size: Int) -> Slice<UnionOrder>
    ): UnionOrder? {
        val bestOfAll =
            ignoreFilledTaker(id = id, clientCall = clientCall, platform = null, orderType = orderType, filter = filter)
        if (bestOfAll == null || bestOfAll.platform == PlatformDto.RARIBLE) {
            return bestOfAll
        }
        logger.debug("Order [{}] is not a preferred platform order, checking preferred platform...", bestOfAll)
        val preferredPlatformBestOrder = ignoreFilledTaker(
            id, clientCall, PlatformDto.RARIBLE, orderType = orderType, filter = filter
        )
        val preferredOrderPrice = preferredPlatformBestOrder?.takePriceUsd
        if (preferredOrderPrice != null &&
            bestOfAll.takePriceUsd != null &&
            preferredOrderPrice.compareTo(bestOfAll.takePriceUsd) == 0
        ) {
            return preferredPlatformBestOrder
        }

        logger.debug("Found best order from ALL platforms: [{}]", bestOfAll)
        return bestOfAll
    }

    suspend fun ignoreFilledTaker(
        id: Any,
        clientCall: suspend (platform: PlatformDto?, continuation: String?, size: Int) -> Slice<UnionOrder>,
        platform: PlatformDto?,
        orderType: OrderType,
        filter: OrderFilters
    ): UnionOrder? {
        var order: UnionOrder?
        var continuation: String? = null
        var attempts = 0

        // Initial size is 1 - hope we're lucky and will get valid order from first try
        var size = 1

        do {
            val slice = clientCall(platform, continuation, size)
            order = slice.entities.firstOrNull {
                // TODO important! may affect performance
                BestOrderValidator.isValid(it) && orderFilter(it, filter, orderType)
            }
            continuation = slice.continuation
            attempts++
            // There are rare cases when item/ownership has A LOT of private orders,
            // if first few attempt were failed, we start to search in batches
            if (attempts == SWITCH_TO_BATCH_ATTEMPTS) {
                size = ORDER_BATCH
            }
            if (attempts == WARN_ATTEMPTS) {
                logger.warn("More than $WARN_ATTEMPTS attempt to get best order for [{}]", id)
            }
        } while (continuation != null && order == null && attempts < MAX_ATTEMPTS)
        if (attempts == MAX_ATTEMPTS) {
            logger.warn("Reached max attempts ({}) for getting orders for [{}]", MAX_ATTEMPTS, id)
            return null
        }
        return order
    }

    private fun orderFilter(order: UnionOrder, filter: OrderFilters, orderType: OrderType): Boolean {
        return when (orderType) {
            OrderType.BID -> orderBidFilter(order, filter)
            OrderType.SELL -> orderSellFilter(order, filter)
        }
    }

    private fun orderBidFilter(order: UnionOrder, filter: OrderFilters): Boolean {
        return when (filter) {
            OrderFilters.COLLECTION -> order.take.type.isCollectionAsset()
            OrderFilters.ITEM -> !order.take.type.isCollectionAsset()
            OrderFilters.ALL -> true
        }
    }

    private fun orderSellFilter(order: UnionOrder, filter: OrderFilters): Boolean {
        return when (filter) {
            OrderFilters.COLLECTION -> order.make.type.isCollectionAsset()
            OrderFilters.ITEM -> !order.make.type.isCollectionAsset()
            OrderFilters.ALL -> true
        }
    }

    companion object {

        private const val MAX_ATTEMPTS = 50
        private const val WARN_ATTEMPTS = 5

        private const val SWITCH_TO_BATCH_ATTEMPTS = 2
        private const val ORDER_BATCH = 10

        enum class OrderFilters { ALL, COLLECTION, ITEM }
        enum class OrderType { BID, SELL }
    }
}
