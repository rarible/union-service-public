package com.rarible.protocol.union.enrichment.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.continuation.Slice
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.model.CurrencyId
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOrder
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.util.spent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EnrichmentOrderService(
    private val orderServiceRouter: BlockchainRouter<OrderService>
) {

    private val logger = LoggerFactory.getLogger(EnrichmentOrderService::class.java)

    suspend fun getById(id: OrderIdDto): OrderDto? {
        return try {
            orderServiceRouter.getService(id.blockchain).getOrderById(id.value)
        } catch (e: WebClientResponseProxyException) {
            logger.warn("Unable to retrieve original Order [{}] from indexer: {}, response:", id, e.message, e.data)
            null
        }
    }

    suspend fun fetchOrderIfDiffers(existing: ShortOrder?, order: OrderDto?): OrderDto? {
        // Nothing to download - there is no existing short order
        if (existing == null) {
            return null
        }
        // Full order we already fetched is the same as short Order we want to download - using obtained order here
        if (existing.getIdDto() == order?.id) {
            return order
        }
        // Downloading full order in common case
        return getById(existing.dtoId)
    }

    suspend fun getBestSell(id: ShortItemId, currencyId: CurrencyId): OrderDto? {
        val now = nowMillis()
        val result = withPreferredRariblePlatform { platform ->
            orderServiceRouter.getService(id.blockchain).getSellOrdersByItem(
                platform,
                id.token,
                id.tokenId.toString(),
                null,
                null,
                null,
                1
            )
        }
        logger.info("Fetched best sell Order for Item [{}]: [{}] ({}ms)", id, result?.id, spent(now))
        return result
    }

    suspend fun getBestSells(id: ShortItemId): List<OrderDto> {
        return withPreferredRariblePlatformOrderList { platform ->
            val collectedOrders = collect { continuation ->
                orderServiceRouter.getService(id.blockchain).getSellOrdersByItem(
                    platform,
                    id.token,
                    id.tokenId.toString(),
                    null,
                    null,
                    continuation,
                    1000
                )
            }
            Slice(continuation = null, entities = collectedOrders)
        }
    }

    suspend fun getBestSell(id: ShortOwnershipId, currencyId: CurrencyId): OrderDto? {
        val now = nowMillis()
        val result = withPreferredRariblePlatform { platform ->
            orderServiceRouter.getService(id.blockchain).getSellOrdersByItem(
                platform,
                id.token,
                id.tokenId.toString(),
                id.owner,
                null,
                null,
                1
            )
        }
        logger.info("Fetched best sell Order for Ownership [{}]: [{}] ({}ms)", id, result?.id, spent(now))
        return result
    }

    suspend fun getBestSells(id: ShortOwnershipId): List<OrderDto> {
        return withPreferredRariblePlatformOrderList { platform ->
            val collectedOrders = collect { continuation ->
                orderServiceRouter.getService(id.blockchain).getSellOrdersByItem(
                    platform,
                    id.token,
                    id.tokenId.toString(),
                    id.owner,
                    null,
                    continuation,
                    1000
                )
            }
            Slice(continuation = null, entities = collectedOrders)
        }
    }

    suspend fun getBestBid(id: ShortItemId, currencyId: CurrencyId): OrderDto? {
        val now = nowMillis()
        val result = withPreferredRariblePlatform { platform ->
            orderServiceRouter.getService(id.blockchain).getOrderBidsByItem(
                platform,
                id.token,
                id.tokenId.toString(),
                null,
                null,
                null,
                null,
                null,
                null,
                1
            )
        }
        logger.info("Fetching best bid Order for Item [{}]: [{}] ({}ms)", id, result?.id, spent(now))
        return result
    }

    suspend fun getBestBids(id: ShortItemId): List<OrderDto> {
        return withPreferredRariblePlatformOrderList { platform ->
            val collectedOrders = collect { continuation ->
                orderServiceRouter.getService(id.blockchain).getOrderBidsByItem(
                    platform,
                    id.token,
                    id.tokenId.toString(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    continuation,
                    1000
                )
            }
            Slice(continuation = null, entities = collectedOrders)
        }
    }

    private suspend fun withPreferredRariblePlatform(
        clientCall: suspend (platform: PlatformDto) -> Slice<OrderDto>
    ): OrderDto? {
        val bestOfAll = clientCall(PlatformDto.ALL).entities.firstOrNull()
        logger.debug("Found best order from ALL platforms: [{}]", bestOfAll)
        if (bestOfAll == null || bestOfAll.platform == PlatformDto.RARIBLE) {
            return bestOfAll
        }
        logger.debug("Order [{}] is not a preferred platform order, checking preferred platform...", bestOfAll)
        val preferredPlatformBestOrder = clientCall(PlatformDto.RARIBLE).entities.firstOrNull()
        logger.debug("Checked preferred platform for best order: [{}]")
        return preferredPlatformBestOrder ?: bestOfAll
    }

    private suspend fun withPreferredRariblePlatformOrderList(
        clientCall: suspend (platform: PlatformDto) -> Slice<OrderDto>
    ): List<OrderDto> {
        val orders = clientCall(PlatformDto.ALL).entities

        logger.debug("Found orders from ALL platforms: [{}]", orders.idsString)
        if (orders.isEmpty() || orders.none { order -> order.platform == PlatformDto.RARIBLE }) {
            return orders
        }
        logger.debug("Order [{}] is not a preferred platform order, checking preferred platform...", orders.idsString)
        val preferredPlatformBestOrder = clientCall(PlatformDto.RARIBLE).entities
        logger.debug("Checked preferred platform for best order: [{}]", preferredPlatformBestOrder.idsString)
        return preferredPlatformBestOrder.ifEmpty { orders }
    }

    private suspend fun collect(
        clientCall: suspend (continuation: String?) -> Slice<OrderDto>
    ): List<OrderDto> {
        var continuation: String? = null
        val orders = mutableListOf<OrderDto>()

        do {
            val slice = clientCall(continuation)
            continuation = slice.continuation
            orders.addAll(slice.entities)
        } while (slice.entities.isNotEmpty() && continuation != null)

        return orders
    }

    private val List<OrderDto>.idsString: String
        get() = joinToString { order -> order.id.toString() }
}
