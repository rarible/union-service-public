package com.rarible.protocol.union.core.tezos.service

import com.rarible.protocol.tezos.api.client.OrderControllerApi
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.core.tezos.converter.TezosOrderConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import kotlinx.coroutines.reactive.awaitFirst

class TezosOrderService(
    override val blockchain: BlockchainDto,
    private val orderControllerApi: OrderControllerApi,
    private val tezosOrderConverter: TezosOrderConverter
) : AbstractBlockchainService(blockchain), OrderService {

    override suspend fun getOrdersAll(
        platform: PlatformDto?, // TODO TEZOS doesn't support it
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getOrdersAll(
            origin,
            size,
            continuation
        ).awaitFirst()
        return tezosOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getOrderById(id: String): OrderDto {
        val order = orderControllerApi.getOrderByHash(id).awaitFirst()
        return tezosOrderConverter.convert(order, blockchain)
    }

    override suspend fun getOrdersByIds(orderIds: List<String>): List<OrderDto> {
        // TODO TEZOS support this method
        return orderIds
            .map { orderControllerApi.getOrderByHash(it).awaitFirst() }
            .map { tezosOrderConverter.convert(it, blockchain) }

        /*val orderIdsDto = OrderIdsDto(
            ids = orderIds.map { EthConverter.convertToWord(it) }
        )
        val orders = orderControllerApi.getOrdersByIds(orderIdsDto).collectList().awaitFirst()
        return orders.map { tezosOrderConverter.convert(it, blockchain) }*/
    }

    override suspend fun getOrderBidsByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?, // TODO TEZOS doesn't support it
        start: Long?, // TODO TEZOS doesn't support it
        end: Long?, // TODO TEZOS doesn't support it
        currencyAddress: String, // TODO TEZOS doesn't support it
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        // TODO add currency filter
        val orders = orderControllerApi.getOrderBidsByItem(
            contract,
            tokenId,
            maker,
            origin,
            size,
            continuation
        ).awaitFirst()
        return tezosOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getOrderBidsByMaker(
        platform: PlatformDto?, // TODO TEZOS doesn't support it
        maker: String,
        origin: String?,
        status: List<OrderStatusDto>?, // TODO TEZOS doesn't support it
        start: Long?, // TODO TEZOS doesn't support it
        end: Long?, // TODO TEZOS doesn't support it
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getOrderBidsByMaker(
            maker,
            origin,
            size,
            continuation
        ).awaitFirst()
        return tezosOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?, // TODO TEZOS doesn't support it
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getSellOrders(
            origin,
            size,
            continuation
        ).awaitFirst()
        return tezosOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getSellOrdersByCollection(
        platform: PlatformDto?, // TODO TEZOS doesn't support it
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getSellOrdersByCollection(
            collection,
            origin,
            size,
            continuation
        ).awaitFirst()
        return tezosOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getSellOrdersByItem(
        platform: PlatformDto?, // TODO TEZOS doesn't support it
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?, // TODO TEZOS doesn't support it
        currencyAddress: String, // TODO TEZOS doesn't support it
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getSellOrderByItem(
            contract,
            tokenId,
            maker,
            origin,
            size,
            continuation
        ).awaitFirst()
        return tezosOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getSellOrdersByMaker(
            maker,
            origin,
            size,
            continuation
        ).awaitFirst()
        return tezosOrderConverter.convert(orders, blockchain)
    }
}
