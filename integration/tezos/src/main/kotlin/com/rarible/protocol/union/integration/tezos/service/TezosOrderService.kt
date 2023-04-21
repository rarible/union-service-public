package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupConverter
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderService
import java.util.regex.Pattern

@CaptureSpan(type = "blockchain")
open class TezosOrderService(
    private val dipdupOrderService: DipdupOrderService,
) : AbstractBlockchainService(BlockchainDto.TEZOS), OrderService {

    override suspend fun getOrdersAll(
        continuation: String?,
        size: Int,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): Slice<UnionOrder> {
        return dipdupOrderService.getOrdersAll(sort, status, null, continuation, size)
    }

    override suspend fun getAllSync(
        continuation: String?,
        size: Int,
        sort: SyncSortDto?
    ): Slice<UnionOrder> {
        return dipdupOrderService.getOrdersAllSync(continuation, size, sort)
    }

    override suspend fun getOrderById(id: String): UnionOrder {
        return dipdupOrderService.getOrderById(id)
    }

    override suspend fun getOrdersByIds(orderIds: List<String>): List<UnionOrder> {
        val uuidIds = orderIds.filter(::isValidUUID)
        val orders = if (uuidIds.isNotEmpty()) {
            dipdupOrderService.getOrderByIds(uuidIds)
        } else {
            emptyList()
        }
        return orders
    }

    override suspend fun getBidCurrencies(itemId: String): List<UnionAssetType> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        return dipdupOrderService.getBidOrderCurrenciesByItem(contract, tokenId)
    }

    override suspend fun getBidCurrenciesByCollection(collectionId: String): List<UnionAssetType> {
        return dipdupOrderService.getBidOrderCurrenciesByCollection(collectionId)
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
        return dipdupOrderService.getBidOrdersByItem(
            contract,
            tokenId,
            makers?.let { it.first() },
            DipDupConverter.convert(platform),
            currencyAddress,
            status,
            continuation,
            size
        )
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
    ): Slice<UnionOrder> {
        return dipdupOrderService.getBidOrdersByMaker(
            maker = maker,
            platforms = DipDupConverter.convert(platform),
            status = status,
            continuation = continuation,
            size = size
        )
    }

    override suspend fun getSellCurrencies(itemId: String): List<UnionAssetType> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        return dipdupOrderService.getSellOrderCurrenciesByItem(contract, tokenId)
    }

    override suspend fun getSellCurrenciesByCollection(collectionId: String): List<UnionAssetType> {
        return dipdupOrderService.getSellOrderCurrenciesByCollection(collectionId)
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        return dipdupOrderService.getSellOrders(
            origin = origin,
            platforms = DipDupConverter.convert(platform),
            continuation = continuation,
            size = size
        )
    }

    override suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        return dipdupOrderService.getSellOrdersByCollection(
            contract = collection,
            origin = origin,
            platforms = DipDupConverter.convert(platform),
            continuation = continuation,
            size = size
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
    ): Slice<UnionOrder> {
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
    ): Slice<UnionOrder> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        val slice = dipdupOrderService.getSellOrdersByItem(
            contract,
            tokenId,
            maker,
            DipDupConverter.convert(platform),
            currencyId,
            status,
            continuation,
            size
        )
        return slice
    }

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: List<String>,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        return dipdupOrderService.getSellOrdersByMaker(
            maker = maker,
            platforms = DipDupConverter.convert(platform),
            status = status,
            continuation = continuation,
            size = size
        )
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

    override suspend fun getAmmOrdersAll(
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        return Slice.empty()
    }

    private fun isValidUUID(str: String?): Boolean {
        return if (str == null) {
            false
        } else UUID_REGEX_PATTERN.matcher(str).matches()
    }

    companion object {
        private val UUID_REGEX_PATTERN: Pattern =
            Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$")
    }
}
