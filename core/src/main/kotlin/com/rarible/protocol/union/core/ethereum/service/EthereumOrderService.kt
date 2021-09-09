package com.rarible.protocol.union.core.ethereum.service

import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.union.core.ethereum.converter.EthConverter
import com.rarible.protocol.union.core.ethereum.converter.EthUnionOrderConverter
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionOrderDto
import com.rarible.protocol.union.dto.UnionOrdersDto
import kotlinx.coroutines.reactive.awaitFirst

class EthereumOrderService(
    private val blockchain: EthBlockchainDto,
    private val orderControllerApi: OrderControllerApi
) : OrderService {

    override fun getBlockchain() = blockchain.name

    override suspend fun getOrdersAll(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): UnionOrdersDto {
        val orders = orderControllerApi.getOrdersAll(
            origin,
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return EthUnionOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getOrderById(id: String): UnionOrderDto {
        val order = orderControllerApi.getOrderByHash(id).awaitFirst()
        return EthUnionOrderConverter.convert(order, blockchain)
    }

    override suspend fun updateOrderMakeStock(id: String): UnionOrderDto {
        val order = orderControllerApi.updateOrderMakeStock(id).awaitFirst()
        return EthUnionOrderConverter.convert(order, blockchain)
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
        val orders = orderControllerApi.getOrderBidsByItem(
            contract,
            tokenId,
            maker,
            origin,
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return EthUnionOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getOrderBidsByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int?
    ): UnionOrdersDto {
        val orders = orderControllerApi.getOrderBidsByMaker(
            maker,
            origin,
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return EthUnionOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): UnionOrdersDto {
        val orders = orderControllerApi.getSellOrders(
            origin,
            EthConverter.convert(platform),
            continuation, size
        ).awaitFirst()
        return EthUnionOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int?
    ): UnionOrdersDto {
        val orders = orderControllerApi.getSellOrdersByCollection(
            collection,
            origin,
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return EthUnionOrderConverter.convert(orders, blockchain)
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
        val orders = orderControllerApi.getSellOrdersByItem(
            contract,
            tokenId,
            maker,
            origin,
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return EthUnionOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int?
    ): UnionOrdersDto {
        val orders = orderControllerApi.getSellOrdersByMaker(
            maker,
            origin,
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return EthUnionOrderConverter.convert(orders, blockchain)
    }
}