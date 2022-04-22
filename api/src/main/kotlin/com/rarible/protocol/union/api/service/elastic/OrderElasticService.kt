package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.api.service.OrderQueryService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import org.springframework.stereotype.Service

@Service
class OrderElasticService : OrderQueryService {
    override suspend fun getByIds(ids: List<OrderIdDto>): List<OrderDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): Slice<OrderDto> {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }
}