package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.dto.FlowOrderIdsDto
import com.rarible.protocol.dto.FlowOrdersPaginationDto
import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.flow.converter.FlowOrderConverter
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import kotlinx.coroutines.reactive.awaitFirst
import reactor.core.publisher.Mono

class FlowOrderService(
    blockchain: BlockchainDto,
    private val orderControllerApi: FlowOrderControllerApi,
    private val flowOrderConverter: FlowOrderConverter
) : AbstractBlockchainService(blockchain), OrderService {

    override suspend fun getOrdersAll(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        return convert(
            orderControllerApi.getOrdersAll(origin, continuation, size)
        )
    }

    override suspend fun getOrderById(id: String): OrderDto {
        val order = orderControllerApi.getOrderByOrderId(id).awaitFirst()
        return flowOrderConverter.convert(order, blockchain)
    }

    override suspend fun getOrdersByIds(orderIds: List<String>): List<OrderDto> {
        val ids = orderIds.map { it.toLong() }
        return orderControllerApi
            .getOrdersByIds(FlowOrderIdsDto(ids))
            .collectList()
            .awaitFirst()
            .map {
                flowOrderConverter.convert(it, blockchain)
            }
    }

    override suspend fun getOrderBidsByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        currencyAddress: String,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        // TODO add currency support
        return convert(
            orderControllerApi.getOrderBidsByItem(
                contract, tokenId, maker, origin, continuation, size
            )
        )
    }

    override suspend fun getOrderBidsByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        // TODO status not supported
        return convert(
            orderControllerApi.getOrderBidsByMaker(
                maker, origin, continuation, size
            )
        )
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        return convert(
            orderControllerApi.getSellOrders(
                origin, continuation, size
            )
        )
    }

    override suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        return convert(
            orderControllerApi.getSellOrdersByCollection(
                collection, origin, continuation, size
            )
        )
    }

    override suspend fun getSellOrdersByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencyAddress: String,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        // TODO add currency support
        return convert(
            orderControllerApi.getSellOrdersByItem(
                contract, tokenId, maker, origin, continuation, size
            )
        )
    }

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        return convert(
            orderControllerApi.getSellOrdersByMaker(
                maker, origin, continuation, size
            )
        )
    }

    private suspend fun convert(orders: Mono<FlowOrdersPaginationDto>): Slice<OrderDto> {
        return flowOrderConverter.convert(
            orders.awaitFirst(),
            blockchain
        )
    }
}
