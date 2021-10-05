package com.rarible.protocol.union.api.controller.test.mock.flow

import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import io.mockk.every
import reactor.kotlin.core.publisher.toMono

class FlowOrderControllerApiMock(
    private val orderControllerApi: FlowOrderControllerApi
) {

    fun mockGetById(vararg orders: FlowOrderDto) {
        orders.forEach {
            every {
                orderControllerApi.getOrderByOrderId(it.id.toString())
            } returns it.toMono()
        }
    }

    // TODO extend mock when Flow support other methods

}