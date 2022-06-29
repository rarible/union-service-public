package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.OrderContinuation
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexApiClient
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexOrderConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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
        val orders =  client.getAllOrders(continuation, size, sort, status).map {
            orderConverter.convert(it, blockchain)
        }
        return Paging(OrderContinuation.ByLastUpdatedAndIdDesc, orders).getSlice(size)
    }

    override suspend fun getAllSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?
    ): Slice<OrderDto> = Slice.empty()

    override suspend fun getOrderById(id: String): OrderDto {
        val order = client.getOrderById(id.toLong())
        return orderConverter.convert(order, blockchain)
    }

    override suspend fun getOrdersByIds(orderIds: List<String>): List<OrderDto> {
        val orders = orderIds.map {
            coroutineScope {
                async(Dispatchers.IO) { getOrderById(it) }
            }
        }
        return orders.awaitAll()
    }

    override suspend fun getBidCurrencies(itemId: String): List<AssetTypeDto> =
        listOf(EthEthereumAssetTypeDto(BlockchainDto.IMMUTABLEX))

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

    override suspend fun getSellCurrencies(itemId: String): List<AssetTypeDto> =
        listOf(EthEthereumAssetTypeDto(BlockchainDto.IMMUTABLEX))

    override suspend fun getSellCurrenciesByCollection(collectionId: String): List<AssetTypeDto> {
        return emptyList()
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int,
    ): Slice<OrderDto> {
        val orders = client.getSellOrders(continuation, size).map {
            orderConverter.convert(it, blockchain)
        }
        return Paging(OrderContinuation.ByLastUpdatedAndIdDesc, orders).getSlice(size)
    }

    override suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int,
    ): Slice<OrderDto> {
        val orders = client.getSellOrdersByCollection(collection, continuation, size).map {
            orderConverter.convert(it, blockchain)
        }
        return Paging(OrderContinuation.ByLastUpdatedAndIdDesc, orders).getSlice(size)
    }

    override suspend fun getOrderFloorSellsByCollection(
        platform: PlatformDto?, collectionId: String, origin: String?, status: List<OrderStatusDto>?,
        currencyAddress: String, continuation: String?, size: Int
    ): Slice<OrderDto> {
        return Slice.empty()
    }

    override suspend fun getOrderFloorBidsByCollection(
        platform: PlatformDto?, collectionId: String, origin: String?, status: List<OrderStatusDto>?, start: Long?,
        end: Long?, currencyAddress: String, continuation: String?, size: Int
    ): Slice<OrderDto> {
        return Slice.empty()
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
        val orders = client.getSellOrdersByItem(itemId, maker, status, currencyId, continuation, size).map {
            orderConverter.convert(it, blockchain)
        }
        return Paging(OrderContinuation.ByLastUpdatedAndIdDesc, orders).getSlice(size)
    }

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int,
    ): Slice<OrderDto> {
        val orders = client.getSellOrdersByMaker(maker, status, continuation, size).map {
            orderConverter.convert(it, blockchain)
        }
        return Paging(OrderContinuation.ByLastUpdatedAndIdDesc, orders).getSlice(size)
    }
}
