package com.rarible.protocol.union.integration.tezos.mock

import com.rarible.protocol.tezos.api.client.OrderControllerApi
import com.rarible.protocol.tezos.dto.OrderDto
import io.mockk.every
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
}
