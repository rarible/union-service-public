package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.dipdup.client.model.DipDupContinuation
import com.rarible.protocol.tezos.api.client.OrderControllerApi
import com.rarible.protocol.tezos.dto.OrderIdsDto
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
import com.rarible.protocol.union.integration.tezos.converter.TezosConverter
import com.rarible.protocol.union.integration.tezos.converter.TezosOrderConverter
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderService
import kotlinx.coroutines.reactive.awaitFirst
import java.util.regex.Pattern

@CaptureSpan(type = "blockchain")
open class TezosOrderService(
    private val orderControllerApi: OrderControllerApi,
    private val tezosOrderConverter: TezosOrderConverter,
    private val dipdupOrderService: DipdupOrderService
) : AbstractBlockchainService(BlockchainDto.TEZOS), OrderService {

    override suspend fun getOrdersAll(
        continuation: String?,
        size: Int,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): Slice<OrderDto> {

        // We try to get new orders only if we get all legacy and continuation != null
        if (dipdupOrderService.enabled() && continuation != null && isDipDupContinuation(continuation)) {
            val slice = dipdupOrderService.getOrdersAll(sort, status, continuation, size)
            return slice

        } else {
            // We should check legacy orders first
            val orders = orderControllerApi.getOrdersAll(
                null,
                tezosOrderConverter.convert(sort),
                tezosOrderConverter.convert(status),
                size,
                continuation
            ).awaitFirst()
            val slice = tezosOrderConverter.convert(orders, blockchain)

            // If legacy orders ended, we should try to get orders from new indexer
            if (dipdupOrderService.enabled() && slice.entities.size < size) {
                val delta = size - slice.entities.size
                val nextSlice = dipdupOrderService.getOrdersAll(sort, status, null, delta)
                return Slice(
                    continuation = nextSlice.continuation,
                    entities = slice.entities + nextSlice.entities
                )
            }

            return slice
        }
    }

    override suspend fun getAllSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?
    ): Slice<OrderDto> {
        return Slice.empty()
    }

    override suspend fun getOrderById(id: String): OrderDto {
        return if (dipdupOrderService.enabled() && isValidUUID(id)) {
            dipdupOrderService.getOrderById(id)
        } else {
            val order = orderControllerApi.getOrderByHash(id).awaitFirst()
            tezosOrderConverter.convert(order, blockchain)
        }
    }

    override suspend fun getOrdersByIds(orderIds: List<String>): List<OrderDto> {
        if (dipdupOrderService.enabled()) {
            val uuidIds = orderIds.filter(::isValidUUID)
            val orders = dipdupOrderService.getOrderByIds(uuidIds)

            val legacyIds = orderIds.subtract(uuidIds).toList()
            val legacyOrders = orderControllerApi.getOrderByIds(OrderIdsDto(legacyIds))
                .collectList().awaitFirst()
                .map { tezosOrderConverter.convert(it, blockchain) }

            return orders + legacyOrders
        } else {
            val form = OrderIdsDto(orderIds)
            val orders = orderControllerApi.getOrderByIds(form).collectList().awaitFirst()
            return orders.map { tezosOrderConverter.convert(it, blockchain) }
        }
    }

    override suspend fun getBidCurrencies(itemId: String): List<AssetTypeDto> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        val assetTypes = orderControllerApi.getCurrenciesByBidOrdersOfItem(contract, tokenId.toString())
            .awaitFirst()
        return assetTypes.currencies.map { TezosConverter.convert(it, blockchain) }
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
        val orders = orderControllerApi.getOrderBidsByItem(
            contract,
            tokenId.toString(),
            makers?.firstOrNull(), // TODO TEZOS support
            origin,
            currencyAddress,
            tezosOrderConverter.convert(status),
            start,
            end,
            size,
            continuation
        ).awaitFirst()
        return tezosOrderConverter.convert(orders, blockchain)
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
        // TODO TEZOS add status/start/end filtering
        val orders = orderControllerApi.getOrderBidsByMaker(
            maker,
            origin,
            size,
            continuation
        ).awaitFirst()
        return tezosOrderConverter.convert(orders, blockchain)
    }

    override suspend fun getSellCurrencies(itemId: String): List<AssetTypeDto> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        val assetTypes = orderControllerApi.getCurrenciesBySellOrdersOfItem(contract, tokenId.toString())
            .awaitFirst()
        val legacyList = assetTypes.currencies.map { TezosConverter.convert(it, blockchain) }
        if (dipdupOrderService.enabled()) {
            val newList = dipdupOrderService.getSellOrderCurrenciesByItem(contract, tokenId)
            return (legacyList + newList).distinct()
        }
        return legacyList
    }

    override suspend fun getSellCurrenciesByCollection(collectionId: String): List<AssetTypeDto> {
        return if (dipdupOrderService.enabled()) {
            dipdupOrderService.getSellOrderCurrenciesByCollection(collectionId)
        } else emptyList()
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getSellOrders(
            origin,
            size,
            continuation
        ).awaitFirst()
        return tezosOrderConverter.convert(orders, blockchain)
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
            size,
            continuation
        ).awaitFirst()
        return tezosOrderConverter.convert(orders, blockchain)
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

        // We try to get new orders only if we get all legacy and continuation != null
        if (dipdupOrderService.enabled() && continuation != null && isDipDupContinuation(continuation)) {
            val slice = dipdupOrderService.getSellOrdersByItem(contract, tokenId, maker, currencyId, status, continuation, size)
            return slice

        } else {
            // We should check legacy orders first
            val orders = orderControllerApi.getSellOrderByItem(
                contract,
                tokenId.toString(),
                maker,
                origin,
                currencyId,
                tezosOrderConverter.convert(status),
                null,
                null,
                size,
                continuation
            ).awaitFirst()
            val slice = tezosOrderConverter.convert(orders, blockchain)

            // If legacy orders ended, we should try to get orders from new indexer
            if (dipdupOrderService.enabled() && slice.entities.size < size) {
                val delta = size - slice.entities.size
                val nextSlice = dipdupOrderService.getSellOrdersByItem(contract, tokenId, maker, currencyId, status, null, delta)
                return Slice(
                    continuation = nextSlice.continuation,
                    entities = slice.entities + nextSlice.entities
                )
            }

            return slice
        }
    }

    fun isDipDupContinuation(continuation: String?) = continuation?.let { DipDupContinuation.isValid(it) } ?: false

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val orders = orderControllerApi.getSellOrdersByMaker(
            maker,
            origin,
            tezosOrderConverter.convert(status),
            size,
            continuation
        ).awaitFirst()
        return tezosOrderConverter.convert(orders, blockchain)
    }

    private fun isValidUUID(str: String?): Boolean {
        return if (str == null) {
            false
        } else UUID_REGEX_PATTERN.matcher(str).matches()
    }

    companion object {
        private val UUID_REGEX_PATTERN: Pattern = Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$")
    }
}
