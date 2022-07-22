package com.rarible.protocol.union.integration.flow.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.dto.FlowOrderIdsDto
import com.rarible.protocol.flow.nft.api.client.FlowBidOrderControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.flow.converter.FlowConverter
import com.rarible.protocol.union.integration.flow.converter.FlowOrderConverter
import kotlinx.coroutines.reactive.awaitFirst
import java.time.Instant

@CaptureSpan(type = "blockchain")
open class FlowOrderService(
    private val orderControllerApi: FlowOrderControllerApi,
    private val bidControllerApi: FlowBidOrderControllerApi,
    private val flowOrderConverter: FlowOrderConverter
) : AbstractBlockchainService(BlockchainDto.FLOW), OrderService {

    override suspend fun getOrdersAll(
        continuation: String?,
        size: Int,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): Slice<OrderDto> {
        val result = orderControllerApi.getOrdersAllByStatus(
            flowOrderConverter.convert(sort),
            continuation,
            size,
            flowOrderConverter.convert(status)
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }

    override suspend fun getAllSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?
    ): Slice<OrderDto> {
        val result = orderControllerApi.getOrdersSync(
            continuation,
            size,
            flowOrderConverter.convert(sort)
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }

    override suspend fun getOrderById(id: String): OrderDto {
        val order = orderControllerApi.getOrderByOrderId(id).awaitFirst()
        return flowOrderConverter.convert(order, blockchain)
    }

    override suspend fun getOrdersByIds(orderIds: List<String>): List<OrderDto> {
        val ids = orderIds.map { it.toLong() }
        val orders = orderControllerApi.getOrdersByIds(FlowOrderIdsDto(ids)).collectList().awaitFirst()
        return orders.map { flowOrderConverter.convert(it, blockchain) }
    }

    override suspend fun getBidCurrencies(itemId: String): List<AssetTypeDto> {
        val assets = bidControllerApi.getBidCurrencies(itemId)
            .collectList().awaitFirst()
        return assets.map { FlowConverter.convert(it, blockchain).type }
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
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        val result = bidControllerApi.getBidsByItem(
            contract,
            tokenId.toString(),
            flowOrderConverter.convert(status),
            makers,
            origin,
            start?.let { Instant.ofEpochMilli(it) },
            end?.let { Instant.ofEpochMilli(it) },
            currencyAddress,
            continuation,
            size
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }

    override suspend fun getOrderBidsByMaker(
        platform: PlatformDto?,
        maker: List<String>,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val result = bidControllerApi.getOrderBidsByMaker(
            maker,
            flowOrderConverter.convert(status),
            origin,
            start?.let { Instant.ofEpochMilli(it) },
            end?.let { Instant.ofEpochMilli(it) },
            continuation,
            size
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }

    override suspend fun getSellCurrencies(itemId: String): List<AssetTypeDto> {
        val assets = orderControllerApi.getSellCurrencies(itemId)
            .collectList().awaitFirst()
        return assets.map { FlowConverter.convert(it, blockchain).type }
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
        val result = orderControllerApi.getSellOrders(
            origin,
            continuation,
            size
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }

    override suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val result = orderControllerApi.getSellOrdersByCollection(
            collection,
            origin,
            continuation,
            size
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
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
        //Not implemented
        return Slice.empty()
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
        //Not implemented
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
        size: Int
    ): Slice<OrderDto> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        val result = orderControllerApi.getSellOrdersByItemAndByStatus(
            contract,
            tokenId.toString(),
            maker,
            origin,
            continuation,
            size,
            flowOrderConverter.convert(status),
            currencyId
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: List<String>,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val result = orderControllerApi.getSellOrdersByMaker(
            maker,
            origin,
            continuation,
            size
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }
}
