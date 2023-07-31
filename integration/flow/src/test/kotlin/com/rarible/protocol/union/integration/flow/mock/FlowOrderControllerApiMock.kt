package com.rarible.protocol.union.integration.flow.mock

import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.dto.FlowOrderIdsDto
import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import io.mockk.every
import reactor.kotlin.core.publisher.toFlux
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

    fun mockGetByIds(vararg orders: FlowOrderDto) {
        every {
            orderControllerApi.getOrdersByIds(
                FlowOrderIdsDto(orders.map { it.id })
            )
        } returns orders.toFlux()
    }
}
