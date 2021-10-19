package com.rarible.protocol.union.api.controller.test.mock.tezos

import com.rarible.protocol.tezos.api.client.OrderControllerApi
import com.rarible.protocol.tezos.dto.OrderDto
import com.rarible.protocol.tezos.dto.OrderPaginationDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import io.mockk.every
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

class TezosOrderControllerApiMock(
    private val orderControllerApi: OrderControllerApi
) {

    fun mockGetById(vararg orders: OrderDto) {
        orders.forEach {
            every {
                orderControllerApi.getOrderByHash(it.hash)
            } returns it.toMono()
        }
    }

    fun mockGetByIds(vararg orders: OrderDto) {
        // TODO uncomment when supported
        mockGetById(*orders)
        /*val hashes = orders.map { it.hash }
        every {
            orderControllerApi.getOrdersByIds(OrderIdsDto(hashes.toList()))
        } returns orders.toFlux()*/
    }

    fun mockGetSellOrdersByItem(itemId: ItemIdDto, vararg returnOrders: OrderDto) {
        every {
            orderControllerApi.getSellOrderByItem(
                itemId.token.value,
                itemId.tokenId.toString(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Mono.just(OrderPaginationDto(returnOrders.asList(), null))
    }

    fun mockGetSellOrdersByOwnership(ownershipId: OwnershipIdDto, vararg returnOrders: OrderDto) {
        every {
            orderControllerApi.getSellOrderByItem(
                ownershipId.token.value,
                ownershipId.tokenId.toString(),
                ownershipId.owner.value,
                any(),
                any(),
                any()
            )
        } returns Mono.just(OrderPaginationDto(returnOrders.asList(), null))
    }

    fun mockGetBidOrdersByItem(itemId: ItemIdDto, vararg returnOrders: OrderDto) {
        every {
            orderControllerApi.getOrderBidsByItem(
                itemId.token.value,
                itemId.tokenId.toString(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Mono.just(OrderPaginationDto(returnOrders.asList(), null))
    }

}
