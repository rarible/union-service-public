package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.dto.OrderIdsDto
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SyncSortDto
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
        val filteredStatus = filterStatus(status) ?: return Slice.empty()

        val orders = orderControllerApi.getOrdersAllByStatus(
            ethOrderConverter.convert(sort),
            continuation,
            size,
            ethOrderConverter.convert(filteredStatus)
        ).awaitFirst()
        return ethOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getAllSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getAllSync(
            ethOrderConverter.convert(sort),
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
        val orderIdsDto = OrderIdsDto(orderIds)
        val orders = orderControllerApi.getByIds(orderIdsDto).awaitFirst().orders
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

    override suspend fun getBidCurrenciesByCollection(collectionId: String): List<AssetTypeDto> {
        val assetTypes = orderControllerApi.getCurrenciesByBidOrdersOfItem(
            collectionId,
            "-1"
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
            makerAddresses,
            origin,
            EthConverter.convert(platform),
            continuation,
            size,
            ethOrderConverter.convert(status),
            currencyAddress,
            start,
            end
        ).awaitFirst()
        return ethOrderConverter.convert(orders, blockchain)
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
        val orders = orderControllerApi.getOrderBidsByMakerAndByStatus(
            maker.map { EthConverter.convertToAddress(it) },
            origin,
            EthConverter.convert(platform),
            continuation,
            size,
            ethOrderConverter.convert(status),
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

    override suspend fun getSellCurrenciesByCollection(collectionId: String): List<AssetTypeDto> {
        val assetTypes = orderControllerApi.getCurrenciesBySellOrdersOfItem(
            collectionId,
            "-1"
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

    override suspend fun getOrderFloorSellsByCollection(
        platform: PlatformDto?,
        collectionId: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        currencyAddress: String,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        //We use hack here, get item with id = -1,
        //since it doesn't exist, we only get collections
        val itemId = "$collectionId:-1"
        return getSellOrdersByItem(
            platform,
            itemId,
            null,
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
        //We use hack here, get item with id = -1,
        //since it doesn't exist, we only get  collections
        val itemId = "$collectionId:-1"
        return getOrderBidsByItem(
            platform,
            itemId,
            null,
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
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        val filteredStatus = filterStatus(status) ?: return Slice.empty()

        val orders = orderControllerApi.getSellOrdersByItemAndByStatus(
            contract,
            tokenId.toString(),
            maker,
            origin,
            EthConverter.convert(platform),
            continuation,
            size,
            ethOrderConverter.convert(filteredStatus),
            currencyId
        ).awaitFirst()
        return ethOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: List<String>,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val filteredStatus = filterStatus(status) ?: return Slice.empty()
        val orders = orderControllerApi.getSellOrdersByMakerAndByStatus(
            maker.map { EthConverter.convertToAddress(it) },
            origin,
            EthConverter.convert(platform),
            continuation,
            size,
            ethOrderConverter.convert(filteredStatus)
        ).awaitFirst()
        return ethOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getAmmOrdersByItem(
        itemId: String,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        val orders = orderControllerApi.getAmmOrdersByItem(
            contract,
            tokenId.toString(),
            continuation,
            size
        ).awaitFirst()
        return ethOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getAmmOrderItemIds(
        id: String,
        continuation: String?,
        size: Int
    ): Slice<ItemIdDto> {
        val result = orderControllerApi.getAmmOrderItemIds(
            id,
            continuation,
            size
        ).awaitFirst()
        val itemIds = result.ids.map { ItemIdDto(blockchain, it) }
        return Slice(continuation, itemIds)
    }

    override suspend fun getAmmOrdersAll(
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val filteredStatus = filterStatus(status) ?: return Slice.empty()
        val orders = orderControllerApi.getSellOrdersByStatus(
            null,
            com.rarible.protocol.dto.PlatformDto.SUDOSWAP, // TODO temporary solution
            continuation,
            size,
            ethOrderConverter.convert(filteredStatus),
            com.rarible.protocol.dto.OrderSortDto.LAST_UPDATE_DESC
        ).awaitFirst()
        return ethOrderConverter.convert(orders, blockchain)
    }

    /**
     * remove HISTORICAL status from list of statuses for calls that aren't related to bids
     * If HISTORICAL was the only status, return null then, which means call can be omitted
     */
    private fun filterStatus(status: List<OrderStatusDto>?): List<OrderStatusDto>? {
        if (status.isNullOrEmpty()) return emptyList()
        val filtered = status.filter { it != OrderStatusDto.HISTORICAL }
        return filtered.ifEmpty { null }
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
