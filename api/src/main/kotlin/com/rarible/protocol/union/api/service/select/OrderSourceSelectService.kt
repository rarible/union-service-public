package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.api.service.OrderQueryService
import com.rarible.protocol.union.api.service.api.OrderApiService
import com.rarible.protocol.union.api.service.elastic.OrderElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import org.springframework.stereotype.Service

@Service
class OrderSourceSelectService(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val orderApiService: OrderApiService,
    private val orderElasticService: OrderElasticService,
): OrderQueryService {

    override suspend fun getByIds(ids: List<OrderIdDto>): List<OrderDto> {
        return getQuerySource().getByIds(ids)
    }

    override suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): Slice<OrderDto> {
        return getQuerySource().getOrdersAll(blockchains, continuation, size, sort, status)
    }

    override suspend fun getSellOrdersByItem(
        blockchain: BlockchainDto,
        itemId: String,
        platform: PlatformDto?,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        return getQuerySource().getSellOrdersByItem(blockchain, itemId, platform, maker, origin, status, continuation, size)
    }

    override suspend fun getOrderBidsByItem(
        blockchain: BlockchainDto,
        itemId: String,
        platform: PlatformDto?,
        makers: List<String>?,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        return getQuerySource().getOrderBidsByItem(blockchain, itemId, platform, makers, origin, status, start, end, continuation, size)
    }

    private fun getQuerySource(): OrderQueryService {
        return when (featureFlagsProperties.enableOrderQueriesToElasticSearch) {
            true -> orderElasticService
            else -> orderApiService
        }
    }
}
