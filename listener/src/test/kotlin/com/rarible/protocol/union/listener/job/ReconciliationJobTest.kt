package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.core.continuation.Slice
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.OrderServiceRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.service.event.EnrichmentOrderEventService
import com.rarible.protocol.union.listener.test.data.defaultUnionListenerProperties
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReconciliationJobTest {

    private val testPageSize = 50

    private val orderService: OrderService = mockk()
    private val orderServiceRouter: OrderServiceRouter = mockk()
    private val orderEventService: EnrichmentOrderEventService = mockk()

    private val orderReconciliationService = ReconciliationJob(
        orderServiceRouter,
        orderEventService,
        defaultUnionListenerProperties()
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(orderServiceRouter, orderEventService)
        coEvery { orderServiceRouter.getService(BlockchainDto.ETHEREUM) } returns orderService
        coEvery { orderEventService.updateOrder(any()) } returns Unit
    }

    @Test
    fun `reconcile orders - first page`() = runBlocking {
        val nextContinuation = "1_1"
        mockGetOrdersAll(null, testPageSize, mockPagination(nextContinuation, testPageSize))

        val result = orderReconciliationService.reconcileOrders(null, BlockchainDto.ETHEREUM)

        assertEquals(nextContinuation, result)
        coVerify(exactly = testPageSize) { orderEventService.updateOrder(any()) }
    }

    @Test
    fun `reconcile orders - next page`() = runBlocking {
        val lastContinuation = "1_1"
        val nextContinuation = "2_2"
        mockGetOrdersAll(lastContinuation, testPageSize, mockPagination(nextContinuation, testPageSize))

        val result = orderReconciliationService.reconcileOrders(lastContinuation, BlockchainDto.ETHEREUM)

        assertEquals(nextContinuation, result)
        coVerify(exactly = testPageSize) { orderEventService.updateOrder(any()) }
    }

    @Test
    fun `reconcile orders - last page`() = runBlocking {
        val lastContinuation = "1_1"
        val nextContinuation = null
        mockGetOrdersAll(lastContinuation, testPageSize, mockPagination(nextContinuation, 50))

        val result = orderReconciliationService.reconcileOrders(lastContinuation, BlockchainDto.ETHEREUM)

        assertNull(result)
        coVerify(exactly = 50) { orderEventService.updateOrder(any()) }
    }

    @Test
    fun `reconcile orders - empty page`() = runBlocking {
        mockGetOrdersAll(null, testPageSize, mockPagination("1_1", 0))

        val result = orderReconciliationService.reconcileOrders(null, BlockchainDto.ETHEREUM)

        assertNull(result)
        coVerify(exactly = 0) { orderEventService.updateOrder(any()) }
    }

    private fun mockGetOrdersAll(continuation: String?, size: Int, result: Slice<OrderDto>): Unit {
        coEvery {
            orderService.getOrdersAll(PlatformDto.ALL, null, continuation, size)
        } returns result
    }

    private fun mockPagination(continuation: String?, count: Int): Slice<OrderDto> {
        val orders = ArrayList<OrderDto>()
        for (i in 1..count) {
            orders.add(mockk())
        }
        return Slice(continuation, orders)
    }
}