package com.rarible.protocol.union.integration.immutablex.scanner

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.integration.data.randomImxOrder
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexActivityEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexItemEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexOrderEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexOwnershipEventHandler
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImxScannerTest {

    private val imxEventsApi: ImxEventsApi = mockk()
    private val imxScanStateRepository: ImxScanStateRepository = mockk()

    private val activityHandler: ImmutablexActivityEventHandler =
        mockk { coEvery { handle(any()) } returns Unit }

    private val ownershipEventHandler: ImmutablexOwnershipEventHandler =
        mockk { coEvery { handle(any()) } returns Unit }

    private val itemEventHandler: ImmutablexItemEventHandler =
        mockk { coEvery { handle(any()) } returns Unit }

    private val orderEventHandler: ImmutablexOrderEventHandler =
        mockk { coEvery { handle(any()) } returns Unit }

    private val imxScanMetrics: ImxScanMetrics = mockk {
        coEvery { onScanError(any(), any()) } returns Unit
        coEvery { onStateUpdated(any()) } returns Unit
    }

    private val scanner = ImxScanner(
        imxEventsApi,
        imxScanStateRepository,
        imxScanMetrics,
        activityHandler,
        ownershipEventHandler,
        itemEventHandler,
        orderEventHandler
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(imxEventsApi, imxScanStateRepository)
        coEvery { imxScanStateRepository.updateState(any(), any()) } returns Unit
        coEvery { imxScanStateRepository.updateState(any(), any(), any()) } returns Unit
    }

    @Test
    fun `scan orders`() {
        val lastUpdated = nowMillis()
        val lastOrder = randomImxOrder().copy(updatedAt = lastUpdated)
        val lastId = lastOrder.orderId.toString()
        val oldOrder = randomImxOrder().copy(updatedAt = lastUpdated.minusSeconds(1))

        val scanState = ImxScanState(
            id = ImxScanEntityType.ORDERS.name,
            entityId = "0",
            entityDate = lastUpdated.minusSeconds(10)
        )

        coEvery { imxScanStateRepository.getOrCreateState(ImxScanEntityType.ORDERS) }
            .returns(scanState)
            .andThen(scanState.copy(entityId = lastId, entityDate = lastUpdated))

        coEvery { imxEventsApi.orders(scanState.entityDate!!, scanState.entityId!!) } returns listOf(
            oldOrder, lastOrder
        )
        coEvery { imxEventsApi.orders(lastUpdated, lastId) } returns emptyList()

        scanner.orders()

        // Only two orders from first request have been handled
        coVerify(exactly = 2) { orderEventHandler.handle(any()) }
        // State updated twice, after second scanning should be interrupted
        coVerify(exactly = 2) { imxScanStateRepository.updateState(any(), lastUpdated, lastId) }
    }

}