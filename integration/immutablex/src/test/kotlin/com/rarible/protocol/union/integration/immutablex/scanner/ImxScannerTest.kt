package com.rarible.protocol.union.integration.immutablex.scanner

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.integration.data.randomImxAsset
import com.rarible.protocol.union.integration.data.randomImxOrder
import com.rarible.protocol.union.integration.data.randomImxTrade
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
import org.assertj.core.api.Assertions.assertThat
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
        collectionEventHandler,
        3000
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

    @Test
    fun `scan trades`() {
        val lastDate = nowMillis().minusSeconds(5)
        val currentId = "100"

        val scanState = ImxScanState(
            id = ImxScanEntityType.TRADE.name,
            entityId = currentId,
            entityDate = lastDate
        )

        val trade1 = randomImxTrade(transactionId = 101, date = lastDate.plusMillis(200))
        val trade2 = randomImxTrade(transactionId = 102, date = lastDate.plusMillis(500))

        coEvery { imxScanStateRepository.getState(ImxScanEntityType.TRADE) }
            .returns(scanState)
            .andThen(scanState.copy(entityId = trade1.activityId.value, entityDate = trade1.timestamp))
            .andThen(scanState.copy(entityId = trade2.activityId.value, entityDate = trade2.timestamp))

        coEvery { imxEventsApi.trades(scanState.entityId) } returns listOf(trade1)
        coEvery { imxEventsApi.trades(trade1.transactionId.toString()) } returns listOf(trade2)
        coEvery { imxEventsApi.trades(trade2.transactionId.toString()) } returns listOf()

        scanner.trades()

        coVerify(exactly = 1) { activityHandler.handle(listOf(trade1)) }
        coVerify(exactly = 1) { activityHandler.handle(listOf(trade2)) }

        // State updated three times, after third scanning should be interrupted
        coVerify(exactly = 1) { imxScanStateRepository.updateState(any(), trade1.timestamp, trade1.activityId.value) }
        coVerify(exactly = 2) { imxScanStateRepository.updateState(any(), trade2.timestamp, trade2.activityId.value) }
    }

    @Test
    fun `scan trades - filtered`() {
        val currentDate = nowMillis().minusSeconds(5)
        val currentId = "100"

        val scanState = ImxScanState(
            id = ImxScanEntityType.TRADE.name,
            entityId = currentId,
            entityDate = currentDate
        )

        val first = randomImxTrade(transactionId = 101, date = currentDate.plusMillis(200))
        val second = randomImxTrade(transactionId = 102, date = currentDate.plusMillis(500))
        val young = randomImxTrade(transactionId = 103, date = currentDate.plusMillis(4000))
        val missing = randomImxTrade(transactionId = 104, date = currentDate.plusMillis(4100))
        val last = randomImxTrade(transactionId = 105, date = currentDate.plusMillis(4200))

        coEvery { imxScanStateRepository.getState(ImxScanEntityType.TRADE) }
            .returns(scanState)
            .andThen(scanState.copy(entityId = second.activityId.value, entityDate = second.timestamp))

        // first scan - one of trades are missing
        coEvery { imxEventsApi.trades(scanState.entityId) } returns listOf(first, second, /* missing,*/ young, last)
        scanner.trades()

        assertThat(scanner.imxBugTraps[ImxScanEntityType.TRADE]!!.lastWarning).isNull()

        // second scan - missing record found
        coEvery { imxEventsApi.trades(second.activityId.value) } returns listOf(young, missing, last)
        scanner.trades()

        // Missing record detected
        assertThat(scanner.imxBugTraps[ImxScanEntityType.TRADE]!!.lastWarning).isNotNull()

        // Only first batch handled, other were filtered
        coVerify(exactly = 1) { activityHandler.handle(listOf(first, second)) }
        coVerify(exactly = 2) { imxScanStateRepository.updateState(any(), second.timestamp, second.activityId.value) }
    }
}
