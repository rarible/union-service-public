package com.rarible.protocol.union.integration.immutablex.scanner

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.integration.data.randomImxAsset
import com.rarible.protocol.union.integration.data.randomImxOrder
import com.rarible.protocol.union.integration.immutablex.handlers.ImxActivityEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImxCollectionEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImxItemEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImxOrderEventHandler
import com.rarible.protocol.union.integration.immutablex.model.ImxScanState
import com.rarible.protocol.union.integration.immutablex.repository.ImxScanStateRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import scalether.domain.Address

class ImxScannerTest {

    private val imxEventsApi: ImxEventsApi = mockk()
    private val imxScanStateRepository: ImxScanStateRepository = mockk()

    private val activityHandler: ImxActivityEventHandler =
        mockk { coEvery { handle(any()) } returns Unit }

    private val itemEventHandler: ImxItemEventHandler =
        mockk { coEvery { handle(any()) } returns Unit }

    private val collectionEventHandler: ImxCollectionEventHandler =
        mockk { coEvery { handle(any()) } returns Unit }

    private val orderEventHandler: ImxOrderEventHandler =
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
        itemEventHandler,
        orderEventHandler,
        collectionEventHandler
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
            id = ImxScanEntityType.ORDER.name,
            entityId = "0",
            entityDate = lastUpdated.minusSeconds(10)
        )

        coEvery { imxScanStateRepository.getState(ImxScanEntityType.ORDER) }
            .returns(scanState)
            .andThen(scanState.copy(entityId = lastId, entityDate = lastUpdated))

        coEvery { imxEventsApi.orders(scanState.entityDate, scanState.entityId) } returns listOf(
            oldOrder, lastOrder
        )
        coEvery { imxEventsApi.orders(lastUpdated, lastId) } returns emptyList()

        scanner.orders()

        // Only two orders from first request have been handled
        coVerify(exactly = 2) { orderEventHandler.handle(any()) }
        // State updated twice, after second scanning should be interrupted
        coVerify(exactly = 2) { imxScanStateRepository.updateState(any(), lastUpdated, lastId) }
    }

    @Test
    fun `scan assets`() {

        val lastUpdated = nowMillis()
        val lastAsset = randomImxAsset().copy(updatedAt = lastUpdated)
        val lastId = lastAsset.itemId
        val oldAsset = randomImxAsset().copy(updatedAt = lastUpdated.minusSeconds(1))

        val scanState = ImxScanState(
            id = ImxScanEntityType.ITEM.name,
            entityId = "${Address.ZERO().prefixed()}:0",
            entityDate = lastUpdated.minusSeconds(10)
        )

        coEvery { imxScanStateRepository.getState(ImxScanEntityType.ITEM) }
            .returns(scanState)
            .andThen(scanState.copy(entityId = lastId, entityDate = lastUpdated))

        coEvery { imxEventsApi.assets(scanState.entityDate, scanState.entityId) } returns listOf(
            oldAsset, lastAsset
        )
        coEvery { imxEventsApi.assets(lastUpdated, lastId) } returns emptyList()

        scanner.assets()

        // Only two assets from first request have been handled
        coVerify(exactly = 2) { itemEventHandler.handle(any()) }
        // State updated twice, after second scanning should be interrupted
        coVerify(exactly = 2) { imxScanStateRepository.updateState(any(), lastUpdated, lastId) }

    }

}