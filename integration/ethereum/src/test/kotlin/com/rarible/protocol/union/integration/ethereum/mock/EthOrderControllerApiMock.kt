package com.rarible.protocol.union.integration.ethereum.mock

import com.rarible.protocol.dto.AssetTypeDto
import com.rarible.protocol.dto.OrderCurrenciesDto
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderIdsDto
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import io.mockk.every
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

class EthOrderControllerApiMock(
    private val orderControllerApi: OrderControllerApi
) {

    fun mockGetById(vararg orders: OrderDto) {
        orders.forEach {
            every {
                orderControllerApi.getOrderByHash(it.hash.prefixed())
            } returns it.toMono()
        }
    }

    fun mockGetByIds(vararg orders: OrderDto) {
        val hashes = orders.map { it.hash.toString() }
        every {
            orderControllerApi.getByIds(OrderIdsDto(hashes))
        } returns OrdersPaginationDto(orders = orders.toList()).toMono()
    }

    fun mockGetSellOrdersByItemAndByStatus(
        itemId: ItemIdDto,
        currencyId: String,
        vararg returnOrders: OrderDto
    ) {
        mockGetSellOrdersByItemAndByStatus(itemId, currencyId, null, *returnOrders)
    }

    fun mockGetSellOrdersByItemAndByStatus(
        itemId: ItemIdDto,
        currencyId: String,
        origin: String?,
        vararg returnOrders: OrderDto
    ) {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
        every {
            orderControllerApi.getSellOrdersByItemAndByStatus(
                eq(contract),
                eq(tokenId.toString()),
                any(),
                origin?.let { eq(it) } ?: isNull(),
                any(),
                any(),
                any(),
                any(),
                eq(currencyId)
            )
        } returns Mono.just(OrdersPaginationDto(returnOrders.asList(), null))
    }

    fun mockGetSellOrdersByItemAndByStatus(
        ownershipId: OwnershipIdDto,
        currencyId: String,
        vararg returnOrders: OrderDto
    ) {
        mockGetSellOrdersByItemAndByStatus(ownershipId, currencyId, null, *returnOrders)
    }

    fun mockGetSellOrdersByItemAndByStatus(
        ownershipId: OwnershipIdDto,
        currencyId: String,
        origin: String?,
        vararg returnOrders: OrderDto
    ) {
        val (contract, tokenId) = CompositeItemIdParser.split(ownershipId.itemIdValue)
        every {
            orderControllerApi.getSellOrdersByItemAndByStatus(
                eq(contract),
                eq(tokenId.toString()),
                eq(ownershipId.owner.value),
                origin?.let { eq(it) } ?: isNull(),
                any(),
                any(),
                eq(1),
                eq(listOf(OrderStatusDto.ACTIVE)),
                eq(currencyId)
            )
        } returns Mono.just(OrdersPaginationDto(returnOrders.asList(), null))
    }

    fun mockGetOrderBidsByItemAndByStatus(
        itemId: ItemIdDto,
        currencyId: String,
        vararg returnOrders: OrderDto
    ) {
        return mockGetOrderBidsByItemAndByStatus(itemId, currencyId, null, *returnOrders)
    }

    fun mockGetOrderBidsByItemAndByStatus(
        itemId: ItemIdDto,
        currencyId: String,
        origin: String?,
        vararg returnOrders: OrderDto
    ) {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
        every {
            orderControllerApi.getOrderBidsByItemAndByStatus(
                eq(contract),
                eq(tokenId.toString()),
                any(),
                origin?.let { eq(it) } ?: isNull(),
                any(),
                any(),
                eq(1),
                eq(listOf(OrderStatusDto.ACTIVE)),
                eq(currencyId),
                any(),
                any()
            )
        } returns Mono.just(OrdersPaginationDto(returnOrders.asList(), null))
    }

    fun mockGetCurrenciesByBidOrdersOfItem(itemId: ItemIdDto, vararg returnTypes: AssetTypeDto) {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
        every {
            orderControllerApi.getCurrenciesByBidOrdersOfItem(
                contract,
                tokenId.toString()
            )
        } returns Mono.just(OrderCurrenciesDto(OrderCurrenciesDto.OrderType.BID, returnTypes.asList()))
    }

    fun mockGetCurrenciesBySellOrdersOfItem(itemId: ItemIdDto, vararg returnTypes: AssetTypeDto) {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
        every {
            orderControllerApi.getCurrenciesBySellOrdersOfItem(
                contract,
                tokenId.toString()
            )
        } returns Mono.just(OrderCurrenciesDto(OrderCurrenciesDto.OrderType.SELL, returnTypes.asList()))
    }

    fun mockGetAmmOrdersByItem(itemId: ItemIdDto, vararg returnOrders: OrderDto) {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
        every {
            orderControllerApi.getAmmOrdersByItem(
                eq(contract),
                eq(tokenId.toString()),
                any(),
                any()
            )
        } returns Mono.just(OrdersPaginationDto(returnOrders.asList(), null))
    }

}
