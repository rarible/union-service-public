package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.api.service.OrderQueryService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import org.springframework.stereotype.Service

@Service
class OrderElasticService : OrderQueryService {
    override suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): OrdersDto {
        TODO("Not yet implemented")
    }

    override suspend fun getAllSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?
    ): OrdersDto {
        throw UnsupportedOperationException("Operation is not supported for Elastic Search")
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
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override suspend fun getSellOrders(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }
}
