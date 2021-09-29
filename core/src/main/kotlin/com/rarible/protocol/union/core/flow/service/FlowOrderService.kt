package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.union.core.continuation.Slice
import com.rarible.protocol.union.core.flow.converter.FlowOrderConverter
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.PlatformDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.reactive.awaitFirst

class FlowOrderService(
    blockchain: BlockchainDto,
    private val orderControllerApi: FlowOrderControllerApi
) : AbstractFlowService(blockchain), OrderService {

    override suspend fun getOrdersAll(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        return stub()
    }

    override suspend fun getOrderById(id: String): OrderDto {
        val order = orderControllerApi.getOrderByOrderId(id).awaitFirst()
        return FlowOrderConverter.convert(order, blockchain)
    }

    override fun getOrdersByIds(orerIds: List<String>): Flow<OrderDto> {
        // TODO implement when Flow support it
        return emptyFlow()
        /*
        val orders = orderControllerApi.getOrdersByIds(orerIds)
        return orders.map { EthOrderConverter.convert(it, blockchain) }.asFlow()
         */
    }

    override suspend fun getOrderBidsByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        return stub()
    }

    override suspend fun getOrderBidsByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        return stub()
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        return stub()
    }

    override suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        return stub()
    }

    override suspend fun getSellOrdersByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        return stub()
    }

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        return stub()
    }

    // TODO remove when FLow support Order API
    private fun stub(): Slice<OrderDto> {
        return Slice(
            continuation = null,
            entities = listOf()
        )
    }
}