package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.tezos.api.client.OrderControllerApi
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.converter.UnionConverter
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.integration.tezos.converter.TezosOrderConverter
import kotlinx.coroutines.reactive.awaitFirst

@CaptureSpan(type = "network", subtype = "tezos")
open class TezosOrderService(
    private val orderControllerApi: OrderControllerApi,
    private val tezosOrderConverter: TezosOrderConverter
) : AbstractBlockchainService(BlockchainDto.TEZOS), OrderService {

    override suspend fun getOrdersAll(
        platform: PlatformDto?,
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
        // TODO TEZOS implement
        return orderIds
            .map { orderControllerApi.getOrderByHash(it).awaitFirst() }
            .map { tezosOrderConverter.convert(it, blockchain) }
    }

    override suspend fun getBidCurrencies(contract: String, tokenId: String): List<AssetTypeDto> {
        // TODO TEZOS implement
        TODO("Not yet implemented")
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
        // TODO TEZOS add currency/status/start/end filtering
        val orders = orderControllerApi.getOrderBidsByItem(
            contract,
            UnionConverter.convertToBigInteger(tokenId).toString(),
            maker,
            origin,
            size,
            continuation
        ).awaitFirst()
        return tezosOrderConverter.convert(orders, blockchain)
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
        // TODO TEZOS add status/start/end filtering
        val orders = orderControllerApi.getOrderBidsByMaker(
            maker,
            origin,
            size,
            continuation
        ).awaitFirst()
        return tezosOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getSellCurrencies(contract: String, tokenId: String): List<AssetTypeDto> {
        // TODO TEZOS implement
        TODO("Not yet implemented")
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
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
        platform: PlatformDto?,
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
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencyId: String,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        // TODO TEZOS add status/currency filtering
        val orders = orderControllerApi.getSellOrderByItem(
            contract,
            UnionConverter.convertToBigInteger(tokenId).toString(),
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
