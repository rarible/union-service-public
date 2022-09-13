package com.rarible.protocol.union.integration.tezos.mock

import com.rarible.dipdup.client.OrderClient
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.client.model.DipDupOrdersPage
import io.mockk.coEvery

class TezosOrderControllerApiMock(
    private val orderControllerApi: OrderClient
) {

    fun mockGetById(vararg orders: DipDupOrder) {
        orders.forEach {
            coEvery {
                orderControllerApi.getOrderById(it.id)
            } returns it
        }
    }

    fun mockGetAll(orders: List<DipDupOrder>) {
        coEvery { orderControllerApi.getOrdersAll(any(), any(), any(), any(), any())
        } returns DipDupOrdersPage(orders)
    }
}
