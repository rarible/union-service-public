package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.union.core.flow.converter.FlowUnionOrderConverter
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionOrderDto
import com.rarible.protocol.union.dto.UnionOrdersDto
import kotlinx.coroutines.reactive.awaitFirst

class FlowOrderService(
    private val blockchain: FlowBlockchainDto,
    private val orderControllerApi: FlowOrderControllerApi
) : OrderService {

    override fun getBlockchain() = blockchain.name

    override suspend fun getOrdersAll(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): UnionOrdersDto {
        return stub()
    }

    override suspend fun getOrderById(id: String): UnionOrderDto {
        val order = orderControllerApi.getOrderByOrderId(id).awaitFirst()
        return FlowUnionOrderConverter.convert(order, blockchain)
    }

    override suspend fun updateOrderMakeStock(id: String): UnionOrderDto {
        // TODO implement when Flow support it
        val order = orderControllerApi.getOrderByOrderId(id).awaitFirst()
        return FlowUnionOrderConverter.convert(order, blockchain)
    }

    override suspend fun getOrderBidsByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): UnionOrdersDto {
        return stub()
    }

    override suspend fun getOrderBidsByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int?
    ): UnionOrdersDto {
        return stub()
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): UnionOrdersDto {
        return stub()
    }

    override suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int?
    ): UnionOrdersDto {
        return stub()
    }

    override suspend fun getSellOrdersByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): UnionOrdersDto {
        return stub()
    }

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int?
    ): UnionOrdersDto {
        return stub()
    }

    // TODO remove when FLow support Order API
    private fun stub(): UnionOrdersDto {
        return UnionOrdersDto(
            continuation = null,
            orders = listOf()
        )
    }
}