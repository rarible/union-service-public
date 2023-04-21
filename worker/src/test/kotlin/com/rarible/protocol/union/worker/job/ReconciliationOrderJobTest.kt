package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderEventService
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrder
import com.rarible.protocol.union.worker.config.ReconciliationProperties
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReconciliationOrderJobTest {

    private val testPageSize = 50

    private val orderService: OrderService = mockk()
    private val orderServiceRouter: BlockchainRouter<OrderService> = mockk()
    private val orderEventService: EnrichmentOrderEventService = mockk()

    private val orderReconciliationService = ReconciliationOrderJob(
        orderServiceRouter,
        orderEventService,
        mockk<WorkerProperties>() {
            every { reconciliation } returns ReconciliationProperties()
        }
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(orderServiceRouter, orderEventService)
        coEvery { orderServiceRouter.getService(BlockchainDto.ETHEREUM) } returns orderService
        coEvery { orderEventService.updateOrder(any(), any()) } returns Unit
    }

    @Test
    fun `run reconciliation task`() = runBlocking {
        mockGetOrdersAll(null, testPageSize, mockPagination("1_1", testPageSize))
        mockGetOrdersAll("1_1", testPageSize, mockPagination("1_2", testPageSize))
        mockGetOrdersAll("1_2", testPageSize, mockPagination(null, 10))

        val result = orderReconciliationService.reconcile(null, BlockchainDto.ETHEREUM).toList()

        assertEquals(2, result.size)
        assertEquals("1_1", result[0])
        assertEquals("1_2", result[1])
    }

    @Test
    fun `reconcile orders - first page`() = runBlocking {
        val nextContinuation = "1_1"
        mockGetOrdersAll(null, testPageSize, mockPagination(nextContinuation, testPageSize))

        val result = orderReconciliationService.reconcileBatch(null, BlockchainDto.ETHEREUM)

        assertEquals(nextContinuation, result)
        coVerify(exactly = testPageSize) { orderEventService.updateOrder(any(), any()) }
    }

    @Test
    fun `reconcile orders - next page`() = runBlocking {
        val lastContinuation = "1_1"
        val nextContinuation = "2_2"
        mockGetOrdersAll(lastContinuation, testPageSize, mockPagination(nextContinuation, testPageSize))

        val result = orderReconciliationService.reconcileBatch(lastContinuation, BlockchainDto.ETHEREUM)

        assertEquals(nextContinuation, result)
        coVerify(exactly = testPageSize) { orderEventService.updateOrder(any(), any()) }
    }

    @Test
    fun `reconcile orders - last page`() = runBlocking {
        val lastContinuation = "1_1"
        val nextContinuation = null
        mockGetOrdersAll(lastContinuation, testPageSize, mockPagination(nextContinuation, 50))

        val result = orderReconciliationService.reconcileBatch(lastContinuation, BlockchainDto.ETHEREUM)

        assertNull(result)
        coVerify(exactly = 50) { orderEventService.updateOrder(any(), any()) }
    }

    @Test
    fun `reconcile orders - empty page`() = runBlocking {
        mockGetOrdersAll(null, testPageSize, mockPagination("1_1", 0))

        val result = orderReconciliationService.reconcileBatch(null, BlockchainDto.ETHEREUM)

        assertNull(result)
        coVerify(exactly = 0) { orderEventService.updateOrder(any(), any()) }
    }

    private fun mockGetOrdersAll(continuation: String?, size: Int, result: Slice<UnionOrder>): Unit {
        coEvery {
            orderService.getOrdersAll(
                continuation,
                size,
                eq(OrderSortDto.LAST_UPDATE_DESC),
                eq(listOf(OrderStatusDto.ACTIVE))
            )
        } returns result
    }

    private fun mockPagination(continuation: String?, count: Int): Slice<UnionOrder> {
        val orders = ArrayList<UnionOrder>()
        for (i in 1..count) {
            orders.add(randomUnionSellOrder())
        }
        return Slice(continuation, orders)
    }
}
