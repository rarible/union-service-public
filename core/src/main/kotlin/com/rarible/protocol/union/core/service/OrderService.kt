package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionOrderDto
import com.rarible.protocol.union.dto.UnionOrdersDto

interface OrderService : BlockchainService {

    suspend fun getOrdersAll(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): UnionOrdersDto

    suspend fun getOrderById(
        id: String
    ): UnionOrderDto

    suspend fun updateOrderMakeStock(
        id: String
    ): UnionOrderDto

    suspend fun getOrderBidsByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        continuation: String?,
        size: Int
    ): UnionOrdersDto

    suspend fun getOrderBidsByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): UnionOrdersDto

    suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): UnionOrdersDto

    suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): UnionOrdersDto

    suspend fun getSellOrdersByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        continuation: String?,
        size: Int
    ): UnionOrdersDto

    suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): UnionOrdersDto
}