package com.rarible.protocol.union.enrichment.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOrder
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.util.spent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = "service", subtype = "enrichment")
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

    suspend fun fetchOrderIfDiffers(existing: ShortOrder?, orders: Map<OrderIdDto, OrderDto>): OrderDto? {
        // Nothing to download - there is no existing short order
        if (existing == null) {
            return null
        }
        // Full order we already fetched is the same as short Order we want to download - using obtained order here
        val theSameOrder = orders[existing.dtoId]
        if (theSameOrder != null) {
            return theSameOrder
        }
        // Downloading full order in common case
        return getById(existing.dtoId)
    }

    suspend fun getBestSell(id: ShortItemId, currencyId: String): OrderDto? {
        val now = nowMillis()
        val result = withPreferredRariblePlatform { platform ->
            orderServiceRouter.getService(id.blockchain).getSellOrdersByItem(
                platform,
                id.token,
                id.tokenId.toString(),
                null,
                null,
                listOf(OrderStatusDto.ACTIVE),
                currencyId,
                null,
                1
            )
        }
        logger.info("Fetched best sell Order for Item [{}]: [{}] ({}ms)", id, result?.id, spent(now))
        return result
    }

    suspend fun getBestSell(id: ShortOwnershipId, currencyId: String): OrderDto? {
        val now = nowMillis()
        val result = withPreferredRariblePlatform { platform ->
            orderServiceRouter.getService(id.blockchain).getSellOrdersByItem(
                platform,
                id.token,
                id.tokenId.toString(),
                id.owner,
                null,
                listOf(OrderStatusDto.ACTIVE),
                currencyId,
                null,
                1
            )
        }
        logger.info("Fetched best sell Order for Ownership [{}]: [{}] ({}ms)", id, result?.id, spent(now))
        return result
    }

    suspend fun getBestBid(id: ShortItemId, currencyId: String): OrderDto? {
        val now = nowMillis()
        val result = withPreferredRariblePlatform { platform ->
            orderServiceRouter.getService(id.blockchain).getOrderBidsByItem(
                platform,
                id.token,
                id.tokenId.toString(),
                null,
                null,
                listOf(OrderStatusDto.ACTIVE),
                null,
                null,
                currencyId,
                null,
                1
            )
        }
        logger.info("Fetching best bid Order for Item [{}]: [{}] ({}ms)", id, result?.id, spent(now))
        return result
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
}
