package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.OrderContinuation
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrderClient
import com.rarible.protocol.union.integration.immutablex.client.TokenIdDecoder
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexOrderConverter

class ImmutablexOrderService(
    private val orderClient: ImmutablexOrderClient
) : AbstractBlockchainService(BlockchainDto.IMMUTABLEX), OrderService {

    private val defaultCurrencies = listOf(EthEthereumAssetTypeDto(blockchain))

    override suspend fun getOrdersAll(
        continuation: String?,
        size: Int,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?,
    ): Slice<OrderDto> {
        val orders = orderClient.getAllOrders(
            continuation,
            size,
            sort,
            status
        ).map { ImmutablexOrderConverter.convert(it, blockchain) }

        val continuationFactory = when (sort) {
            OrderSortDto.LAST_UPDATE_ASC -> OrderContinuation.ByLastUpdatedAndIdAsc
            OrderSortDto.LAST_UPDATE_DESC, null -> OrderContinuation.ByLastUpdatedAndIdDesc
        }

        return Paging(continuationFactory, orders).getSlice(size)
    }

    override suspend fun getAllSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?
    ): Slice<OrderDto> {
        val safeSort = when (sort) {
            SyncSortDto.DB_UPDATE_ASC -> OrderSortDto.LAST_UPDATE_ASC
            SyncSortDto.DB_UPDATE_DESC -> OrderSortDto.LAST_UPDATE_DESC
            null -> OrderSortDto.LAST_UPDATE_ASC
        }
        return getOrdersAll(continuation, size, safeSort, null)
    }

    override suspend fun getOrderById(id: String): OrderDto {
        val order = orderClient.getById(id)
        return ImmutablexOrderConverter.convert(order, blockchain)
    }

    override suspend fun getOrdersByIds(orderIds: List<String>): List<OrderDto> {
        return orderClient.getByIds(orderIds).map {
            ImmutablexOrderConverter.convert(it, blockchain)
        }
    }

    // TODO IMMUTABLEX we need to support ERC20 here?
    override suspend fun getBidCurrencies(itemId: String): List<AssetTypeDto> {
        return defaultCurrencies
    }

    // IMX doesn't support floor orders
    override suspend fun getBidCurrenciesByCollection(collectionId: String): List<AssetTypeDto> {
        return emptyList()
    }

    // TODO IMMUTABLEX we can support it for non-active statuses
    override suspend fun getOrderBidsByItem(
        platform: PlatformDto?,
        itemId: String,
        makers: List<String>?,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?, // Not supported for bids by IMX
        end: Long?, // Not supported for bids by IMX
        currencyAddress: String,
        continuation: String?,
        size: Int,
    ): Slice<OrderDto> {
        val (token, tokenId) = IdParser.split(TokenIdDecoder.decodeItemId(itemId), 2)
        val orders = orderClient
            .getBuyOrdersByItem(token, tokenId, makers, status, currencyAddress, continuation, size)
            .map { ImmutablexOrderConverter.convert(it, blockchain) }

        return Paging(OrderContinuation.ByBidPriceUsdAndIdDesc, orders).getSlice(size)
    }

    override suspend fun getOrderBidsByMaker(
        platform: PlatformDto?,
        maker: List<String>,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?, // Not supported for bids by IMX
        end: Long?, // Not supported for bids by IMX
        continuation: String?,
        size: Int,
    ): Slice<OrderDto> {
        val orders = orderClient.getBuyOrdersByMaker(maker, status, continuation, size)
            .map { ImmutablexOrderConverter.convert(it, blockchain) }

        return Paging(OrderContinuation.ByLastUpdatedAndIdDesc, orders).getSlice(size)
    }

    // TODO IMMUTABLEX we need to support ERC20 here?
    override suspend fun getSellCurrencies(itemId: String): List<AssetTypeDto> {
        return defaultCurrencies
    }

    // IMX doesn't support floor orders
    override suspend fun getSellCurrenciesByCollection(collectionId: String): List<AssetTypeDto> {
        return emptyList()
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int,
    ): Slice<OrderDto> {
        val orders = orderClient.getSellOrders(continuation, size).map {
            ImmutablexOrderConverter.convert(it, blockchain)
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
        val orders = orderClient.getSellOrdersByCollection(collection, continuation, size).map {
            ImmutablexOrderConverter.convert(it, blockchain)
        }
        return Paging(OrderContinuation.ByLastUpdatedAndIdDesc, orders).getSlice(size)
    }

    // IMX doesn't support floor orders
    override suspend fun getOrderFloorSellsByCollection(
        platform: PlatformDto?, collectionId: String, origin: String?, status: List<OrderStatusDto>?,
        currencyAddress: String, continuation: String?, size: Int
    ): Slice<OrderDto> {
        return Slice.empty()
    }

    // IMX doesn't support floor orders
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
        size: Int
    ): Slice<OrderDto> {
        val (token, tokenId) = IdParser.split(TokenIdDecoder.decodeItemId(itemId), 2)
        val orders = orderClient.getSellOrdersByItem(
            token,
            tokenId,
            maker,
            status,
            currencyId,
            continuation,
            size
        ).map { ImmutablexOrderConverter.convert(it, blockchain) }

        return Paging(OrderContinuation.BySellPriceUsdAndIdAsc, orders).getSlice(size)
    }

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: List<String>,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int,
    ): Slice<OrderDto> {
        val orders = orderClient.getSellOrdersByMaker(maker, status, continuation, size).map {
            ImmutablexOrderConverter.convert(it, blockchain)
        }
        return Paging(OrderContinuation.ByLastUpdatedAndIdDesc, orders).getSlice(size)
    }
}
