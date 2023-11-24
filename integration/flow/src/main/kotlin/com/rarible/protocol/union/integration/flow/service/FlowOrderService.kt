package com.rarible.protocol.union.integration.flow.service

import com.rarible.protocol.dto.FlowOrderIdsDto
import com.rarible.protocol.flow.nft.api.client.FlowBidOrderControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.core.model.UnionAmmTradeInfo
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderFormDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.flow.converter.FlowConverter
import com.rarible.protocol.union.integration.flow.converter.FlowOrderConverter
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import java.time.Instant

open class FlowOrderService(
    private val orderControllerApi: FlowOrderControllerApi,
    private val bidControllerApi: FlowBidOrderControllerApi,
    private val flowOrderConverter: FlowOrderConverter
) : AbstractBlockchainService(BlockchainDto.FLOW), OrderService {

    override suspend fun upsertOrder(form: OrderFormDto): UnionOrder {
        throw UnionException("Off-Chain orders not supported by $blockchain")
    }

    override suspend fun getOrdersAll(
        continuation: String?,
        size: Int,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): Slice<UnionOrder> {
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
    ): Slice<UnionOrder> {
        val result = orderControllerApi.getOrdersSync(
            continuation,
            size,
            flowOrderConverter.convert(sort)
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }

    override suspend fun getOrderById(id: String): UnionOrder {
        val order = orderControllerApi.getOrderByOrderId(id).awaitFirst()
        return flowOrderConverter.convert(order, blockchain)
    }

    override suspend fun getValidatedOrderById(id: String): UnionOrder {
        return getOrderById(id)
    }

    override suspend fun getOrdersByIds(orderIds: List<String>): List<UnionOrder> {
        val orders = orderControllerApi.getOrdersByIds(FlowOrderIdsDto(orderIds)).collectList().awaitFirst()
        return orders.map { flowOrderConverter.convert(it, blockchain) }
    }

    override suspend fun getAmmOrderTradeInfo(id: String, itemCount: Int): UnionAmmTradeInfo {
        throw UnionException("Operation is not supported for $blockchain")
    }

    override suspend fun getBidCurrencies(
        itemId: String,
        status: List<OrderStatusDto>?
    ): List<UnionAssetType> {
        // TODO FLOW add filter by status
        val assets = bidControllerApi.getBidCurrencies(itemId)
            .collectList().awaitFirst()
        return assets.map { FlowConverter.convert(it, blockchain).type }
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
        currencyAddresses: List<String>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        val result = bidControllerApi.getOrderBidsByMaker(
            maker,
            origin,
            flowOrderConverter.convert(status),
            start?.let { Instant.ofEpochMilli(it) },
            end?.let { Instant.ofEpochMilli(it) },
            continuation,
            size
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }

    override suspend fun getSellCurrencies(
        itemId: String,
        status: List<OrderStatusDto>?
    ): List<UnionAssetType> {
        // TODO FLOW add filter by status
        val assets = orderControllerApi.getSellCurrencies(itemId)
            .collectList().awaitFirst()
        return assets.map { FlowConverter.convert(it, blockchain).type }
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
    ): Slice<UnionOrder> {
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
    ): Slice<UnionOrder> {
        // Not implemented
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
    ): Slice<UnionOrder> {
        // Not implemented
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
    ): Slice<UnionOrder> {
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
    ): Slice<UnionOrder> {
        val result = orderControllerApi.getSellOrdersByMaker(
            maker,
            origin,
            continuation,
            size
        ).awaitFirst()
        return flowOrderConverter.convert(result, blockchain)
    }

    override suspend fun getAmmOrdersByItem(
        itemId: String,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        return Slice.empty()
    }

    override suspend fun getAmmOrderItemIds(
        id: String,
        continuation: String?,
        size: Int
    ): Slice<ItemIdDto> {
        return Slice.empty()
    }

    override suspend fun cancelOrder(id: String): UnionOrder {
        throw UnionException("Operation is not supported for $blockchain")
    }

    override suspend fun reportOrder(id: String) {
        logger.info("Reported flow order: $id")
    }

    override suspend fun getAmmOrdersAll(
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        return Slice.empty()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FlowOrderService::class.java)
    }
}
