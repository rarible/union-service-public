package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.core.model.PoolItemAction
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderEventService
import com.rarible.protocol.union.enrichment.test.data.randomSudoSwapAmmDataV1Dto
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.worker.config.ReconciliationProperties
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReconciliationPoolOrderJobTest {

    private val testPageSize = 8

    private val orderService: OrderService = mockk { every { blockchain } returns BlockchainDto.ETHEREUM }
    private val orderEventService: EnrichmentOrderEventService = mockk()
    private val orderServiceRouter: BlockchainRouter<OrderService> = BlockchainRouter(
        listOf(orderService),
        listOf(BlockchainDto.ETHEREUM)
    )

    private val job = ReconciliationPoolOrderJob(
        orderServiceRouter,
        orderEventService,
        mockk<WorkerProperties>() {
            every { reconciliation } returns ReconciliationProperties()
        }
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(orderEventService)
        coEvery { orderEventService.updatePoolOrderPerItem(any(), any(), any(), any()) } returns Unit
    }

    @Test
    fun `run reconciliation task`() = runBlocking<Unit> {
        val page1 = mockPagination("1", testPageSize)
        val page2 = mockPagination("2", testPageSize)
        val page3 = mockPagination(null, 1)

        val items = mockPoolItemIds(page1.entities) + mockPoolItemIds(page2.entities) + mockPoolItemIds(page3.entities)

        mockGetOrdersAll(null, page1)
        mockGetOrdersAll("1", page2)
        mockGetOrdersAll("2", page3)

        val result = job.reconcile(null, BlockchainDto.ETHEREUM).toList()

        assertThat(result.size).isEqualTo(2)
        assertThat(result[0]).isEqualTo("1")
        assertThat(result[1]).isEqualTo("2")

        coVerify(exactly = items.size) {
            orderEventService.updatePoolOrderPerItem(
                any(),
                any(),
                eq(PoolItemAction.INCLUDED),
                any()
            )
        }
        items.forEach {
            coVerify(exactly = 1) {
                orderEventService.updatePoolOrderPerItem(
                    it.value,
                    it.key,
                    eq(PoolItemAction.INCLUDED),
                    any()
                )
            }
        }
    }

    private fun mockGetOrdersAll(continuation: String?, result: Slice<OrderDto>) {
        coEvery {
            orderService.getAmmOrdersAll(
                eq(listOf(OrderStatusDto.ACTIVE)),
                continuation,
                testPageSize
            )
        } returns result
    }

    private fun mockPagination(continuation: String?, count: Int): Slice<OrderDto> {
        val orders = ArrayList<OrderDto>()
        for (i in 1..count) {
            orders.add(
                randomUnionSellOrderDto().copy(
                    data = randomSudoSwapAmmDataV1Dto()
                )
            )
        }
        return Slice(continuation, orders)
    }

    private fun mockPoolItemIds(orders: List<OrderDto>): Map<ItemIdDto, OrderDto> {
        val result = HashMap<ItemIdDto, OrderDto>()
        orders.forEach { order ->
            val itemIds = (0..5).map { randomEthItemId() }
            itemIds.forEach { result[it] = order }
            coEvery { orderService.getAmmOrderItemIds(order.id.value, any(), any()) } returns Slice(null, itemIds)
        }
        return result
    }
}
