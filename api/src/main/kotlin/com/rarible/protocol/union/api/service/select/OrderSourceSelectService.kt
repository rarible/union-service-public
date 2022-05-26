package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.enrichment.service.query.order.OrderQueryService
import com.rarible.protocol.union.enrichment.service.query.order.OrderApiMergeService
import com.rarible.protocol.union.api.service.elastic.OrderElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdsDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import org.springframework.stereotype.Service

@Service
class OrderSourceSelectService(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val orderApiService: OrderApiMergeService,
    private val orderElasticService: OrderElasticService,
) : OrderQueryService {

    /**
     * Should always route to OrderApiService
     */
    suspend fun getOrderById(id: String): OrderDto {
        return orderApiService.getOrderById(id)
    }

    /**
     * Should always route to OrderApiService
     */
    suspend fun getByIds(orderIdsDto: OrderIdsDto): List<OrderDto> {
        return orderApiService.getByIds(orderIdsDto)
    }

    override suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): OrdersDto {
        return getQuerySource().getOrdersAll(blockchains, continuation, size, sort, status)
    }

    override suspend fun getAllSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?
    ): OrdersDto {
        return orderApiService.getAllSync(blockchain, continuation, size, sort)
    }

    override suspend fun getSellOrdersByItem(
        itemId: String,
        platform: PlatformDto?,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        return getQuerySource().getSellOrdersByItem(itemId, platform, maker, origin, status, continuation, size)
    }

    override suspend fun getOrderBidsByItem(
        itemId: String,
        platform: PlatformDto?,
        maker: List<String>?,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        return getQuerySource().getOrderBidsByItem(
            itemId,
            platform,
            maker,
            origin,
            status,
            start,
            end,
            continuation,
            size
        )
    }

    override suspend fun getOrderBidsByMaker(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        return getQuerySource().getOrderBidsByMaker(
            blockchains,
            platform,
            maker,
            origin,
            status,
            start,
            end,
            continuation,
            size
        )
    }

    override suspend fun getSellOrders(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        return getQuerySource().getSellOrders(blockchains, platform, origin, continuation, size)
    }

    override suspend fun getSellOrdersByMaker(
        maker: String,
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?
    ): OrdersDto {
        return getQuerySource().getSellOrdersByMaker(maker, blockchains, platform, origin, continuation, size, status)
    }

    private fun getQuerySource(): OrderQueryService {
        return when (featureFlagsProperties.enableOrderQueriesToElasticSearch) {
            true -> orderElasticService
            else -> orderApiService
        }
    }
}
