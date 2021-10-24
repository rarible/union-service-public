package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.protocol.dto.OrderIdsDto
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.integration.ethereum.EthereumComponent
import com.rarible.protocol.union.integration.ethereum.PolygonComponent
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

sealed class EthOrderService(
    override val blockchain: BlockchainDto,
    private val orderControllerApi: OrderControllerApi,
    private val ethOrderConverter: EthOrderConverter
) : AbstractBlockchainService(blockchain), OrderService {

    override suspend fun getOrdersAll(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getOrdersAll(
            origin,
            EthConverter.convert(platform),
            continuation,
            size
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

    override suspend fun getBidCurrencies(
        contract: String,
        tokenId: String
    ): List<AssetTypeDto> {
        val assetTypes = orderControllerApi.getCurrenciesByBidOrdersOfItem(
            contract,
            tokenId
        ).awaitFirst()
        return assetTypes.currencies.map { EthConverter.convert(it, blockchain) }
    }

    override suspend fun getOrderBidsByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        currencyAddress: String,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getOrderBidsByItemAndByStatus(
            contract,
            tokenId,
            ethOrderConverter.convert(status),
            maker,
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
        contract: String,
        tokenId: String
    ): List<AssetTypeDto> {
        val assetTypes = orderControllerApi.getCurrenciesBySellOrdersOfItem(
            contract,
            tokenId
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
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencyId: String,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getSellOrdersByItemAndByStatus(
            contract,
            tokenId,
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

@Component
@EthereumComponent
class EthereumOrderService(
    @Qualifier("ethereum.order.api") OrderControllerApi: OrderControllerApi,
    ethOrderConverter: EthOrderConverter
) : EthOrderService(
    BlockchainDto.ETHEREUM,
    OrderControllerApi,
    ethOrderConverter
)

@Component
@PolygonComponent
class PolygonOrderService(
    @Qualifier("polygon.order.api") OrderControllerApi: OrderControllerApi,
    ethOrderConverter: EthOrderConverter
) : EthOrderService(
    BlockchainDto.POLYGON,
    OrderControllerApi,
    ethOrderConverter
)
