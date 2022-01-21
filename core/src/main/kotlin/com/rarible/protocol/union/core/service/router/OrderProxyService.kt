package com.rarible.protocol.union.core.service.router

import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.page.Slice

class OrderProxyService(
    val orderService: OrderService,
    private val supportedPlatforms: Set<PlatformDto>
) : OrderService {

    override val blockchain = orderService.blockchain

    override suspend fun getOrdersAll(
        continuation: String?,
        size: Int,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): Slice<OrderDto> {
        return orderService.getOrdersAll(
            continuation,
            size,
            sort,
            status
        )
    }

    override suspend fun getOrderById(id: String): OrderDto {
        return orderService.getOrderById(id)
    }

    override suspend fun getOrdersByIds(orderIds: List<String>): List<OrderDto> {
        return orderService.getOrdersByIds(orderIds)
    }

    override suspend fun getBidCurrencies(contract: String, tokenId: String): List<AssetTypeDto> {
        return orderService.getBidCurrencies(contract, tokenId)
    }

    override suspend fun getOrderBidsByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        makers: List<String>?,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        currencyAddress: String,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        if (!isPlatformSupported(platform)) return Slice.empty()
        return orderService.getOrderBidsByItem(
            platform,
            contract,
            tokenId,
            makers,
            origin,
            status,
            start,
            end,
            currencyAddress,
            continuation,
            size
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
        if (!isPlatformSupported(platform)) return Slice.empty()
        return orderService.getOrderBidsByMaker(
            platform,
            maker,
            origin,
            status,
            start,
            end,
            continuation,
            size
        )
    }

    override suspend fun getSellCurrencies(contract: String, tokenId: String): List<AssetTypeDto> {
        return orderService.getSellCurrencies(contract, tokenId)
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        if (!isPlatformSupported(platform)) return Slice.empty()
        return orderService.getSellOrders(
            platform,
            origin,
            continuation,
            size
        )
    }

    override suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        if (!isPlatformSupported(platform)) return Slice.empty()
        return orderService.getSellOrdersByCollection(
            platform,
            collection,
            origin,
            continuation,
            size
        )
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
        if (!isPlatformSupported(platform)) return Slice.empty()
        return orderService.getSellOrdersByItem(
            platform,
            contract,
            tokenId,
            maker,
            origin,
            status,
            currencyId,
            continuation,
            size
        )
    }

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        if (!isPlatformSupported(platform)) return Slice.empty()
        return orderService.getSellOrdersByMaker(
            platform,
            maker,
            origin,
            continuation,
            size
        )
    }

    private fun isPlatformSupported(platform: PlatformDto?): Boolean {
        if (platform == null) {
            return true
        }
        return supportedPlatforms.contains(platform)
    }
}
