package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.dto.OrderIdsDto
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import kotlinx.coroutines.reactive.awaitFirst

open class EthOrderService(
    override val blockchain: BlockchainDto,
    private val orderControllerApi: OrderControllerApi,
    private val ethOrderConverter: EthOrderConverter
) : AbstractBlockchainService(blockchain), OrderService {

    override suspend fun getOrdersAll(
        continuation: String?,
        size: Int,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getOrdersAllByStatus(
            ethOrderConverter.convert(sort),
            continuation,
            size,
            ethOrderConverter.convert(status)
        ).awaitFirst()
        return ethOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getOrderById(id: String): OrderDto {
        val order = orderControllerApi.getOrderByHash(id).awaitFirst()
        return ethOrderConverter.convert(order, blockchain)
    }

    override suspend fun getOrdersByIds(orderIds: List<String>): List<OrderDto> {
        val orderIdsDto = OrderIdsDto(orderIds.map { EthConverter.convertToWord(it) })
        val orders = orderControllerApi.getOrdersByIds(orderIdsDto).collectList().awaitFirst()
        return orders.map { ethOrderConverter.convert(it, blockchain) }
    }

    override suspend fun getBidCurrencies(itemId: String): List<AssetTypeDto> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        val assetTypes = orderControllerApi.getCurrenciesByBidOrdersOfItem(
            contract,
            tokenId.toString()
        ).awaitFirst()
        return assetTypes.currencies.map { EthConverter.convert(it, blockchain) }
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
        val makerAddresses = makers?.map { EthConverter.convertToAddress(it) }
        val orders = orderControllerApi.getOrderBidsByItemAndByStatus(
            contract,
            tokenId.toString(),
            ethOrderConverter.convert(status),
            makerAddresses,
            origin,
            EthConverter.convert(platform),
            continuation,
            size,
            currencyAddress,
            start,
            end
        ).awaitFirst()
        return ethOrderConverter.convert(orders, blockchain)
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
        val orders = orderControllerApi.getOrderBidsByMakerAndByStatus(
            maker,
            ethOrderConverter.convert(status),
            origin,
            EthConverter.convert(platform),
            continuation,
            size,
            start,
            end
        ).awaitFirst()
        return ethOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getSellCurrencies(
        itemId: String
    ): List<AssetTypeDto> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        val assetTypes = orderControllerApi.getCurrenciesBySellOrdersOfItem(
            contract,
            tokenId.toString()
        ).awaitFirst()
        return assetTypes.currencies.map { EthConverter.convert(it, blockchain) }
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getSellOrders(
            origin,
            EthConverter.convert(platform),
            continuation, size
        ).awaitFirst()
        return ethOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getSellOrdersByCollection(
            collection,
            origin,
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return ethOrderConverter.convert(orders, blockchain)
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
        val orders = orderControllerApi.getSellOrdersByItemAndByStatus(
            contract,
            tokenId.toString(),
            maker,
            origin,
            EthConverter.convert(platform),
            continuation,
            size,
            ethOrderConverter.convert(status),
            currencyId
        ).awaitFirst()
        return ethOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getSellOrdersByMaker(
            maker,
            origin,
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return ethOrderConverter.convert(orders, blockchain)
    }
}

@CaptureSpan(type = "blockchain")
open class EthereumOrderService(
    orderControllerApi: OrderControllerApi,
    ethOrderConverter: EthOrderConverter
) : EthOrderService(
    BlockchainDto.ETHEREUM,
    orderControllerApi,
    ethOrderConverter
)

@CaptureSpan(type = "blockchain")
open class PolygonOrderService(
    orderControllerApi: OrderControllerApi,
    ethOrderConverter: EthOrderConverter
) : EthOrderService(
    BlockchainDto.POLYGON,
    orderControllerApi,
    ethOrderConverter
)
