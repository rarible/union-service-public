package com.rarible.protocol.union.integration.solana.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.solana.api.client.OrderControllerApi
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.solana.converter.SolanaOrderConverter
import kotlinx.coroutines.reactive.awaitFirst

@CaptureSpan(type = "blockchain")
open class SolanaOrderService(
    private val orderApi: OrderControllerApi,
    private val solanaOrderConverter: SolanaOrderConverter
) : AbstractBlockchainService(BlockchainDto.SOLANA), OrderService {

    override suspend fun getOrderById(id: String): OrderDto {
        val order = orderApi.getOrderById(id).awaitFirst()
        return solanaOrderConverter.convert(order, blockchain)
    }

    override suspend fun getOrdersAll(
        continuation: String?,
        size: Int,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): Slice<OrderDto> {
        val orders = orderApi.getOrdersAll(
            continuation,
            size,
            solanaOrderConverter.convert(sort),
            solanaOrderConverter.convert(status)
        ).awaitFirst()
        return solanaOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getOrdersByIds(orderIds: List<String>): List<OrderDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getBidCurrencies(itemId: String): List<AssetTypeDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getOrderBidsByItem(
        platform: PlatformDto?,
        itemId: String,
        makers: List<String>?,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        currencyAddress: String,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override suspend fun getSellCurrencies(itemId: String): List<AssetTypeDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getSellOrdersByItem(
        platform: PlatformDto?,
        itemId: String,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencyId: String,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        TODO("Not yet implemented")
    }
}
