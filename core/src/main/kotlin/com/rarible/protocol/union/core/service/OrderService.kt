package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.model.UnionAmmTradeInfo
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderFormDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.page.Slice

interface OrderService : BlockchainService {

    suspend fun upsertOrder(
        form: OrderFormDto
    ): UnionOrder

    suspend fun getOrdersAll(
        continuation: String?,
        size: Int,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): Slice<UnionOrder>

    suspend fun getAllSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?
    ): Slice<UnionOrder>

    suspend fun getOrderById(
        id: String
    ): UnionOrder

    suspend fun getValidatedOrderById(
        id: String
    ): UnionOrder

    suspend fun getOrdersByIds(
        orderIds: List<String>
    ): List<UnionOrder>

    suspend fun getAmmOrderTradeInfo(
        orderId: String,
        itemCount: Int
    ): UnionAmmTradeInfo

    suspend fun getBidCurrencies(
        itemId: String,
        status: List<OrderStatusDto>?
    ): List<UnionAssetType>

    suspend fun getBidCurrenciesByCollection(
        collectionId: String,
        status: List<OrderStatusDto>?
    ): List<UnionAssetType>

    suspend fun getOrderBidsByItem(
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
    ): Slice<UnionOrder>

    suspend fun getOrderBidsByMaker(
        platform: PlatformDto?,
        maker: List<String>,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencyAddresses: List<String>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder>

    suspend fun getSellCurrencies(
        itemId: String,
        status: List<OrderStatusDto>?
    ): List<UnionAssetType>

    suspend fun getSellCurrenciesByCollection(
        collectionId: String,
        status: List<OrderStatusDto>?
    ): List<UnionAssetType>

    suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder>

    suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder>

    suspend fun getOrderFloorSellsByCollection(
        platform: PlatformDto?,
        collectionId: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencyAddress: String,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder>

    suspend fun getOrderFloorBidsByCollection(
        platform: PlatformDto?,
        collectionId: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        currencyAddress: String,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder>

    suspend fun getSellOrdersByItem(
        platform: PlatformDto?,
        itemId: String,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencyId: String,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder>

    suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: List<String>,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder>

    suspend fun getAmmOrdersAll(
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder>

    suspend fun getAmmOrdersByItem(
        itemId: String,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder>

    suspend fun getAmmOrderItemIds(
        id: String,
        continuation: String?,
        size: Int
    ): Slice<ItemIdDto>

    suspend fun cancelOrder(
        id: String,
    ): UnionOrder
}
