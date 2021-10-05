package com.rarible.protocol.nftorder.api.test.mock

import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderIdsDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.order.api.client.OrderControllerApi
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
        val hashes = orders.map { it.hash }
        every {
            orderControllerApi.getOrdersByIds(OrderIdsDto(hashes.toList()))
        } returns orders.toFlux()
    }

    fun mockGetSellOrdersByItem(itemId: ItemIdDto, vararg returnOrders: OrderDto) {
        every {
            orderControllerApi.getSellOrdersByItem(
                itemId.token.value,
                itemId.tokenId.toString(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Mono.just(OrdersPaginationDto(returnOrders.asList(), null))
    }

    fun mockGetSellOrdersByOwnership(ownershipId: OwnershipIdDto, vararg returnOrders: OrderDto) {
        every {
            orderControllerApi.getSellOrdersByItem(
                ownershipId.token.value,
                ownershipId.tokenId.toString(),
                ownershipId.owner.value,
                any(),
                any(),
                any(),
                any()
            )
        } returns Mono.just(OrdersPaginationDto(returnOrders.asList(), null))
    }

    fun mockGetBidOrdersByItem(itemId: ItemIdDto, vararg returnOrders: OrderDto) {
        every {
            orderControllerApi.getOrderBidsByItem(
                itemId.token.value,
                itemId.tokenId.toString(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Mono.just(OrdersPaginationDto(returnOrders.asList(), null))
    }

}