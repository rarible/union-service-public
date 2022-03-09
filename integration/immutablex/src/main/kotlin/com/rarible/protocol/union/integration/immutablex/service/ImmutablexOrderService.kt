package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexApiClient
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexOrderConverter

class ImmutablexOrderService(
    private val client: ImmutablexApiClient,
    private val orderConverter: ImmutablexOrderConverter
) : AbstractBlockchainService(BlockchainDto.IMMUTABLEX), OrderService {
    override suspend fun getOrdersAll(
        continuation: String?,
        size: Int,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?,
    ): Slice<OrderDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getOrderById(id: String): OrderDto {
        val order = client.getOrderById(id.toLong())
        return orderConverter.convert(order, blockchain)
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
        size: Int,
    ): Slice<OrderDto> = Slice.empty()

    override suspend fun getOrderBidsByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int,
    ): Slice<OrderDto> = Slice.empty()

    override suspend fun getSellCurrencies(itemId: String): List<AssetTypeDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int,
    ): Slice<OrderDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int,
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
        size: Int,
    ): Slice<OrderDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int,
    ): Slice<OrderDto> {
        TODO("Not yet implemented")
    }
}
