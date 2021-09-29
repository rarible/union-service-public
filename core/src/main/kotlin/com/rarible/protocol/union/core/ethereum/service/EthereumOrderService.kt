package com.rarible.protocol.union.core.ethereum.service

import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.union.core.continuation.Slice
import com.rarible.protocol.union.core.ethereum.converter.EthConverter
import com.rarible.protocol.union.core.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.PlatformDto
import kotlinx.coroutines.reactive.awaitFirst

class EthereumOrderService(
    override val blockchain: BlockchainDto,
    private val orderControllerApi: OrderControllerApi
) : AbstractEthereumService(blockchain), OrderService {

    override suspend fun getOrdersAll(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getOrdersAll(
            origin,
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return EthOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getOrderById(id: String): OrderDto {
        val order = orderControllerApi.getOrderByHash(id).awaitFirst()
        return EthOrderConverter.convert(order, blockchain)
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
        val orders = orderControllerApi.getOrderBidsByItem(
            contract,
            tokenId,
            maker,
            origin,
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return EthOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getOrderBidsByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getOrderBidsByMaker(
            maker,
            origin,
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return EthOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getSellOrders(
            origin,
            EthConverter.convert(platform),
            continuation, size
        ).awaitFirst()
        return EthOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getSellOrdersByCollection(
            collection,
            origin,
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return EthOrderConverter.convert(orders, blockchain)
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
        val orders = orderControllerApi.getSellOrdersByItem(
            contract,
            tokenId,
            maker,
            origin,
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return EthOrderConverter.convert(orders, blockchain)
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
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return EthOrderConverter.convert(orders, blockchain)
    }
}