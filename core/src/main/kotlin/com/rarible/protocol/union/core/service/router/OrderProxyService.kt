package com.rarible.protocol.union.core.service.router

import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.page.Slice

class OrderProxyService(
    private val orderService: OrderService,
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

    override suspend fun getAllSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?
    ): Slice<OrderDto> {
        return orderService.getAllSync(
            continuation,
            size,
            sort
        )
    }

    override suspend fun getOrderById(id: String): OrderDto {
        return orderService.getOrderById(id)
    }

    override suspend fun getOrdersByIds(orderIds: List<String>): List<OrderDto> {
        return orderService.getOrdersByIds(orderIds)
    }

    override suspend fun getBidCurrencies(itemId: String): List<AssetTypeDto> {
        return orderService.getBidCurrencies(itemId)
    }

    override suspend fun getBidCurrenciesByCollection(collectionId: String): List<AssetTypeDto> {
        return emptyList()
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
        if (!isPlatformSupported(platform)) return Slice.empty()
        return orderService.getOrderBidsByItem(
            platform,
            itemId,
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

    override suspend fun getSellCurrencies(itemId: String): List<AssetTypeDto> {
        return orderService.getSellCurrencies(itemId)
    }

    override suspend fun getSellCurrenciesByCollection(collectionId: String): List<AssetTypeDto> {
        return emptyList()
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

    override suspend fun getOrderFloorSellsByCollection(
        platform: PlatformDto?,
        collectionId: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencyAddress: String,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        if (!isPlatformSupported(platform)) return Slice.empty()
        return orderService.getOrderFloorSellsByCollection(
            platform,
            collectionId,
            origin,
            status,
            currencyAddress,
            continuation,
            size
        )
    }

    override suspend fun getOrderFloorBidsByCollection(
        platform: PlatformDto?,
        collectionId: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        currencyAddress: String,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        if (!isPlatformSupported(platform)) return Slice.empty()
        return orderService.getOrderFloorBidsByCollection(
            platform,
            collectionId,
            origin,
            status,
            start,
            end,
            currencyAddress,
            continuation,
            size
        )
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
        if (!isPlatformSupported(platform)) return Slice.empty()
        return orderService.getSellOrdersByItem(
            platform,
            itemId,
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
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        if (!isPlatformSupported(platform)) return Slice.empty()
        return orderService.getSellOrdersByMaker(
            platform,
            maker,
            origin,
            status,
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
