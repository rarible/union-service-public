package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto

interface OrderQueryService {

    suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): OrdersDto

    suspend fun getAllSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?
    ): OrdersDto

    suspend fun getSellOrdersByItem(
        itemId: String,
        platform: PlatformDto?,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int?
    ): OrdersDto

    suspend fun getOrderBidsByItem(
        itemId: String,
        platform: PlatformDto?,
        maker: List<String>?,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): OrdersDto

    suspend fun getOrderBidsByMaker(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): OrdersDto

    suspend fun getSellOrders(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): OrdersDto

    suspend fun getSellOrdersByMaker(
        maker: String,
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?
    ): OrdersDto
}
