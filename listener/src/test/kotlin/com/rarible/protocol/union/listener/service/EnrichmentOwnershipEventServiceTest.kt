package com.rarible.protocol.union.listener.service

import com.mongodb.client.result.DeleteResult
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.event.OwnershipEventListener
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.BestOrderService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.test.data.randomEthItemId
import com.rarible.protocol.union.test.data.randomEthOwnershipId
import com.rarible.protocol.union.test.data.randomUnionOrderDto
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EnrichmentOwnershipEventServiceTest {

    private val ownershipService: EnrichmentOwnershipService = mockk()
    private val itemEventService: EnrichmentItemEventService = mockk()
    private val eventListener: OwnershipEventListener = mockk()
    private val ownershipEventListeners = listOf(eventListener)
    private val bestOrderService: BestOrderService = mockk()

    private val ownershipEventService = EnrichmentOwnershipEventService(
        ownershipService,
        itemEventService,
        ownershipEventListeners,
        bestOrderService
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(
            ownershipService,
            itemEventService,
            eventListener,
            bestOrderService
        )
        coEvery { eventListener.onEvent(any()) } returns Unit
        coEvery { itemEventService.onOwnershipUpdated(any(), any()) } returns Unit
    }

    @Test
    fun `on ownership best sell order updated - ownership exists, updated`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val shortOwnership = randomShortOwnership(ownershipId)
        val order = randomUnionOrderDto(itemId, shortOwnership.id.owner)
        val shortOrder = ShortOrderConverter.convert(order)

        val expectedShortOwnership = shortOwnership.copy(bestSellOrder = shortOrder)

        coEvery { ownershipService.get(shortOwnership.id) } returns shortOwnership
        coEvery { bestOrderService.getBestSellOrder(shortOwnership, order) } returns expectedShortOwnership
        coEvery { ownershipService.save(expectedShortOwnership) } returns expectedShortOwnership
        coEvery { ownershipService.enrichOwnership(expectedShortOwnership, null, order) } returns mockk()

        ownershipEventService.onOwnershipBestSellOrderUpdated(shortOwnership.id, order)

        // Listener should be notified, Ownership - saved and Item data should be recalculated
        coVerify(exactly = 1) { eventListener.onEvent(any()) }
        coVerify(exactly = 1) { ownershipService.save(expectedShortOwnership) }
        coVerify(exactly = 1) { itemEventService.onOwnershipUpdated(shortOwnership.id, order) }
        coVerify(exactly = 0) { ownershipService.delete(shortOwnership.id) }
    }

    @Test
    fun `on ownership best sell order updated - ownership doesn't exist, order cancelled`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val shortOwnership = randomShortOwnership(ownershipId)
        val order = randomUnionOrderDto(itemId, shortOwnership.id.owner)
        val shortOrder = ShortOrderConverter.convert(order)

        val expectedOwnership = shortOwnership.copy(bestSellOrder = shortOrder)

        // There is no existing short ownership, and order is cancelled
        coEvery { ownershipService.get(shortOwnership.id) } returns null
        coEvery { bestOrderService.getBestSellOrder(shortOwnership, order) } returns shortOwnership

        ownershipEventService.onOwnershipBestSellOrderUpdated(shortOwnership.id, order)

        // Since Ownership wasn't in DB and received Order is cancelled, we should just skip such update
        coVerify(exactly = 0) { eventListener.onEvent(any()) }
        coVerify(exactly = 0) { ownershipService.save(expectedOwnership) }
        coVerify(exactly = 0) { itemEventService.onOwnershipUpdated(shortOwnership.id, order) }
        coVerify(exactly = 0) { ownershipService.delete(shortOwnership.id) }
    }

    @Test
    fun `on ownership best sell order updated - ownership exist, order cancelled`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val currentShortOrder = ShortOrderConverter.convert(randomUnionOrderDto())
        val shortOwnership = randomShortOwnership(itemId).copy(bestSellOrder = currentShortOrder)
        val order = randomUnionOrderDto(itemId, shortOwnership.id.owner).copy(cancelled = true)

        val expectedShortOwnership = shortOwnership.copy(bestSellOrder = null)

        // Ownership exists, best Order is cancelled - Ownership should be deleted
        coEvery { ownershipService.get(shortOwnership.id) } returns shortOwnership
        // Means order is cancelled
        coEvery { bestOrderService.getBestSellOrder(shortOwnership, order) } returns expectedShortOwnership
        coEvery { ownershipService.delete(shortOwnership.id) } returns DeleteResult.acknowledged(1)
        coEvery { ownershipService.enrichOwnership(expectedShortOwnership, null, order) } returns mockk()

        ownershipEventService.onOwnershipBestSellOrderUpdated(shortOwnership.id, order)

        // Listener should be notified, Ownership - deleted and Item data should be recalculated
        coVerify(exactly = 1) { eventListener.onEvent(any()) }
        coVerify(exactly = 0) { ownershipService.save(shortOwnership) }
        coVerify(exactly = 1) { itemEventService.onOwnershipUpdated(shortOwnership.id, order) }
        coVerify(exactly = 1) { ownershipService.delete(shortOwnership.id) }
    }

    @Test
    fun `on ownership best sell order updated - ownership exist, order not updated`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val temp = randomShortOwnership(itemId)
        val order = randomUnionOrderDto(itemId, temp.id.owner)
        val shortOrder = ShortOrderConverter.convert(order)
        val ownership = temp.copy(bestSellOrder = shortOrder)

        // Ownership exists, best Order is the same - nothing should happen here
        coEvery { ownershipService.get(ownership.id) } returns ownership
        coEvery { bestOrderService.getBestSellOrder(ownership, order) } returns ownership

        ownershipEventService.onOwnershipBestSellOrderUpdated(ownership.id, order)

        // Since nothing changed for Ownership, and it's order, we should skip such update
        coVerify(exactly = 0) { eventListener.onEvent(any()) }
        coVerify(exactly = 0) { ownershipService.save(any()) }
        coVerify(exactly = 0) { itemEventService.onOwnershipUpdated(ownership.id, order) }
        coVerify(exactly = 0) { ownershipService.delete(ownership.id) }
    }

    @Test
    fun `on ownership deleted - success`() = runBlocking {
        val ownershipId = ShortOwnershipId(randomEthOwnershipId())

        coEvery { ownershipService.delete(ownershipId) } returns DeleteResult.acknowledged(1)

        ownershipEventService.onOwnershipDeleted(ownershipId.toDto())

        // Ownership deleted, listeners notified, item recalculated
        coVerify(exactly = 1) { eventListener.onEvent(any()) }
        coVerify(exactly = 1) { ownershipService.delete(ownershipId) }
        coVerify(exactly = 1) { itemEventService.onOwnershipUpdated(ownershipId, null) }
    }

    @Test
    fun `on ownership deleted - nothing to delete`() = runBlocking {
        val ownershipId = ShortOwnershipId(randomEthOwnershipId())

        coEvery { ownershipService.delete(ownershipId) } returns DeleteResult.acknowledged(0)

        ownershipEventService.onOwnershipDeleted(ownershipId.toDto())

        // Even we don't have Ownership in DB, we need to notify listeners, but we should not recalculate Item sell stat
        coVerify(exactly = 1) { eventListener.onEvent(any()) }
        coVerify(exactly = 1) { ownershipService.delete(ownershipId) }
        coVerify(exactly = 0) { itemEventService.onOwnershipUpdated(ownershipId, null) }
    }

}
