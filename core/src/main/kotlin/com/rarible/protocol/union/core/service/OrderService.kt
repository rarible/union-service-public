package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto

interface OrderService : BlockchainService {

    suspend fun getOrdersAll(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): OrdersDto

    suspend fun getOrderById(
        id: String
    ): OrderDto

    suspend fun updateOrderMakeStock(
        id: String
    ): OrderDto

    suspend fun getOrderBidsByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        continuation: String?,
        size: Int
    ): OrdersDto

    suspend fun getOrderBidsByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): OrdersDto

    suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): OrdersDto

    suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): OrdersDto

    suspend fun getSellOrdersByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        continuation: String?,
        size: Int
    ): OrdersDto

    suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): OrdersDto
}