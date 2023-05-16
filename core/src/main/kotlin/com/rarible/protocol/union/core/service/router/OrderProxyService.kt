package com.rarible.protocol.union.core.service.router

import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SyncSortDto
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
    ): Slice<UnionOrder> {
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
    ): Slice<UnionOrder> {
        return orderService.getAllSync(
            continuation,
            size,
            sort
        )
    }

    override suspend fun getOrderById(id: String): UnionOrder {
        return orderService.getOrderById(id)
    }

    override suspend fun getOrdersByIds(orderIds: List<String>): List<UnionOrder> {
        return orderService.getOrdersByIds(orderIds)
    }

    override suspend fun getBidCurrencies(
        itemId: String,
        status: List<OrderStatusDto>?
    ): List<UnionAssetType> {
        return orderService.getBidCurrencies(itemId, status)
    }

    override suspend fun getBidCurrenciesByCollection(
        collectionId: String,
        status: List<OrderStatusDto>?
    ): List<UnionAssetType> {
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
    ): Slice<UnionOrder> {
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
        maker: List<String>,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencyAddresses: List<String>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        if (!isPlatformSupported(platform)) return Slice.empty()
        return orderService.getOrderBidsByMaker(
            platform,
            maker,
            origin,
            status,
            currencyAddresses,
            start,
            end,
            continuation,
            size
        )
    }

    override suspend fun getSellCurrencies(
        itemId: String,
        status: List<OrderStatusDto>?
    ): List<UnionAssetType> {
        return orderService.getSellCurrencies(itemId, status)
    }

    override suspend fun getSellCurrenciesByCollection(
        collectionId: String,
        status: List<OrderStatusDto>?
    ): List<UnionAssetType> {
        return emptyList()
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
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
    ): Slice<UnionOrder> {
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
    ): Slice<UnionOrder> {
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
    ): Slice<UnionOrder> {
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
    ): Slice<UnionOrder> {
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
        maker: List<String>,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
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

    override suspend fun getAmmOrdersAll(
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        return orderService.getAmmOrdersAll(status, continuation, size)
    }

    override suspend fun getAmmOrdersByItem(
        itemId: String,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        return orderService.getAmmOrdersByItem(itemId, continuation, size)
    }

    override suspend fun getAmmOrderItemIds(
        id: String,
        continuation: String?,
        size: Int
    ): Slice<ItemIdDto> {
        return orderService.getAmmOrderItemIds(id, continuation, size)
    }

    private fun isPlatformSupported(platform: PlatformDto?): Boolean {
        if (platform == null) {
            return true
        }
        return supportedPlatforms.contains(platform)
    }
}
