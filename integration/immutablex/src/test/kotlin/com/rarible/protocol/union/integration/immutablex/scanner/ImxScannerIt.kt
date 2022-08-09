package com.rarible.protocol.union.integration.immutablex.scanner

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
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

class ImxScannerIt() {

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

    private val scanner = ImxScanner(
        imxEventsApi,
        imxScanStateRepository,
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
        val scanState = ImxScanState(ImxScanEntityType.ORDERS.name)
        val lastUpdated = nowMillis()
        val lastOrder = randomImxOrder().copy(updatedAt = lastUpdated)
        val oldOrder = randomImxOrder().copy(updatedAt = lastUpdated.minusSeconds(1))
        val cursor = DateIdContinuation(lastUpdated, lastOrder.orderId.toString()).toString()

        coEvery { imxScanStateRepository.getOrCreateState(ImxScanEntityType.ORDERS) }
            .returns(scanState)
            .andThen(scanState.copy(cursor = cursor, entityDate = lastUpdated))

        coEvery { imxEventsApi.orders(null) } returns listOf(oldOrder, lastOrder)
        coEvery { imxEventsApi.orders(cursor) } returns emptyList()

        scanner.orders()

        // Only two orders from first request have been handled
        coVerify(exactly = 2) { orderEventHandler.handle(any()) }
        // State updated twice, after second scanning should be interrupted
        coVerify(exactly = 2) { imxScanStateRepository.updateState(any(), cursor, lastUpdated) }
    }

}