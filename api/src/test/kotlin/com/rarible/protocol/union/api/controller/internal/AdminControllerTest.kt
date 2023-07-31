package com.rarible.protocol.union.api.controller.internal

import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import randomOrderDto
import randomOrderId
import randomUnionOrder

class AdminControllerTest {
    private val service = mockk<OrderService>()
    private val router = mockk<BlockchainRouter<OrderService>>()
    private val enrichmentOrderService = mockk<EnrichmentOrderService>()

    private val controller = AdminController(router, enrichmentOrderService)

    @Test
    fun `cancel order - ok`() = runBlocking<Unit> {
        val orderId = randomOrderId(blockchain = BlockchainDto.TEZOS)
        val canceledUnionOrder = randomUnionOrder()
        val canceledDtoOrder = randomOrderDto()

        every { router.getService(orderId.blockchain) } returns service
        coEvery { service.cancelOrder(orderId.value) } returns canceledUnionOrder
        coEvery { enrichmentOrderService.enrich(canceledUnionOrder) } returns canceledDtoOrder

        val result = controller.cancelOrder(orderId.fullId())
        assertThat(result.body).isEqualTo(canceledDtoOrder)
    }
}
