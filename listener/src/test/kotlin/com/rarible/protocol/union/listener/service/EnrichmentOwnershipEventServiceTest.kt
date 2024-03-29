package com.rarible.protocol.union.listener.service

import com.rarible.protocol.union.core.event.OutgoingOwnershipEventListener
import com.rarible.protocol.union.core.model.PoolItemAction
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.stubEventMark
import com.rarible.protocol.union.core.service.AuctionContractService
import com.rarible.protocol.union.dto.OwnershipSourceDto
import com.rarible.protocol.union.enrichment.converter.OwnershipDtoConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.BestOrderService
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityService
import com.rarible.protocol.union.enrichment.service.EnrichmentAuctionService
import com.rarible.protocol.union.enrichment.service.EnrichmentItemEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.service.ReconciliationEventService
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityBurn
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityMint
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityTransfer
import com.rarible.protocol.union.enrichment.test.data.randomUnionOwnership
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrder
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EnrichmentOwnershipEventServiceTest {

    private val ownershipService: EnrichmentOwnershipService = mockk()
    private val itemService: EnrichmentItemService = mockk()
    private val activityService: EnrichmentActivityService = mockk()
    private val itemEventService: EnrichmentItemEventService = mockk()
    private val eventListener: OutgoingOwnershipEventListener = mockk()
    private val auctionContractService: AuctionContractService = mockk()
    private val enrichmentAuctionService: EnrichmentAuctionService = mockk()
    private val ownershipEventListeners = listOf(eventListener)
    private val bestOrderService: BestOrderService = mockk()
    private val reconciliationEventService: ReconciliationEventService = mockk()

    private val ownershipEventService = EnrichmentOwnershipEventService(
        ownershipService,
        itemService,
        itemEventService,
        enrichmentAuctionService,
        activityService,
        ownershipEventListeners,
        bestOrderService,
        auctionContractService,
        reconciliationEventService
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(
            ownershipService,
            itemEventService,
            eventListener,
            bestOrderService,
            activityService
        )
        coEvery { itemService.getItemOrigins(any()) } returns emptyList()
        coEvery { eventListener.onEvent(any()) } returns Unit
        coEvery { itemEventService.onOwnershipUpdated(any(), any(), any(), any()) } returns Unit
        coEvery { auctionContractService.isAuctionContract(any(), any()) } returns false
        coEvery { enrichmentAuctionService.fetchOwnershipAuction(any()) } returns null
        coEvery { ownershipService.mergeWithAuction(any(), null) } returnsArgument 0
        coEvery { reconciliationEventService.onCorruptedOwnership(any()) } returns Unit
    }

    @Test
    fun `on ownership best sell order updated - ownership exists, updated`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val shortOwnership = randomShortOwnership(ownershipId)
        val order = randomUnionSellOrder(itemId, shortOwnership.id.owner)
        val shortOrder = ShortOrderConverter.convert(order)

        val expectedShortOwnership = shortOwnership.copy(bestSellOrder = shortOrder)

        coEvery { ownershipService.get(shortOwnership.id) } returns shortOwnership
        coEvery {
            bestOrderService.updateBestSellOrder(shortOwnership, order, emptyList())
        } returns expectedShortOwnership
        coEvery { ownershipService.save(expectedShortOwnership) } returns expectedShortOwnership
        coEvery {
            ownershipService.enrichOwnership(
                expectedShortOwnership,
                null,
                listOf(order).associateBy { it.id })
        } returns OwnershipDtoConverter.convert(randomUnionOwnership())

        ownershipEventService.onOwnershipBestSellOrderUpdated(shortOwnership.id, order, stubEventMark())

        // Listener should be notified, Ownership - saved and Item data should be recalculated
        coVerify(exactly = 1) { eventListener.onEvent(any()) }
        coVerify(exactly = 1) { ownershipService.save(expectedShortOwnership) }
        coVerify(exactly = 1) {
            itemEventService.onOwnershipUpdated(
                oldOwnership = shortOwnership,
                newOwnership = expectedShortOwnership,
                order = order,
                eventTimeMarks = any()
            )
        }
        coVerify(exactly = 0) { ownershipService.delete(shortOwnership.id) }
        coVerify(exactly = 0) { reconciliationEventService.onCorruptedOwnership(any()) }
    }

    @Test
    fun `on ownership best sell order updated - ownership doesn't exist, order cancelled`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val shortOwnership = randomShortOwnership(ownershipId)
        val order = randomUnionSellOrder(itemId, shortOwnership.id.owner)

        // There is no existing short ownership, and order is cancelled
        coEvery { ownershipService.get(shortOwnership.id) } returns null
        coEvery {
            bestOrderService.updateBestSellOrder(any<ShortOwnership>(), eq(order), emptyList())
        } returns shortOwnership

        ownershipEventService.onOwnershipBestSellOrderUpdated(shortOwnership.id, order, stubEventMark())

        // Since Ownership wasn't in DB and received Order is cancelled, we should just skip such update
        coVerify(exactly = 0) { eventListener.onEvent(any()) }
        coVerify(exactly = 0) { ownershipService.save(any()) }
        coVerify(exactly = 0) {
            itemEventService.onOwnershipUpdated(
                oldOwnership = null,
                newOwnership = shortOwnership,
                order = order,
                eventTimeMarks = any()
            )
        }
        coVerify(exactly = 0) { ownershipService.delete(shortOwnership.id) }
        coVerify(exactly = 0) { reconciliationEventService.onCorruptedOwnership(any()) }
    }

    @Test
    fun `on ownership best sell order updated - ownership exist, order cancelled`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val currentShortOrder = ShortOrderConverter.convert(randomUnionSellOrder())
        val shortOwnership = randomShortOwnership(itemId).copy(bestSellOrder = currentShortOrder)
        val order = randomUnionSellOrder(itemId, shortOwnership.id.owner).copy(cancelled = true)

        val expectedShortOwnership = shortOwnership.copy(bestSellOrder = null)

        // Ownership exists, best Order is cancelled - Ownership should be deleted
        coEvery { ownershipService.get(shortOwnership.id) } returns shortOwnership
        // Means order is cancelled
        coEvery {
            bestOrderService.updateBestSellOrder(shortOwnership, order, emptyList())
        } returns expectedShortOwnership

        coEvery { ownershipService.save(expectedShortOwnership) } returns expectedShortOwnership

        coEvery {
            ownershipService.enrichOwnership(
                expectedShortOwnership,
                null,
                listOf(order).associateBy { it.id })
        } returns OwnershipDtoConverter.convert(randomUnionOwnership())

        ownershipEventService.onOwnershipBestSellOrderUpdated(shortOwnership.id, order, stubEventMark())

        // Listener should be notified, Ownership - deleted and Item data should be recalculated
        coVerify(exactly = 1) { eventListener.onEvent(any()) }
        coVerify(exactly = 1) { ownershipService.save(expectedShortOwnership) }
        coVerify(exactly = 1) {
            itemEventService.onOwnershipUpdated(
                oldOwnership = shortOwnership,
                newOwnership = expectedShortOwnership,
                order = order,
                eventTimeMarks = any()
            )
        }
        coVerify(exactly = 0) { ownershipService.delete(shortOwnership.id) }
        coVerify(exactly = 0) { reconciliationEventService.onCorruptedOwnership(any()) }
    }

    @Test
    fun `on ownership pool order updated - excluded`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val currentShortOrder = ShortOrderConverter.convert(randomUnionSellOrder())
        val shortOwnership = randomShortOwnership(itemId).copy(bestSellOrder = currentShortOrder)
        val order = randomUnionSellOrder(itemId, shortOwnership.id.owner)
        val hackedOrder = order.copy(status = UnionOrder.Status.FILLED)

        val expectedShortOwnership = shortOwnership.copy(bestSellOrder = null)

        // Ownership exists, best Order is filled (since item removed from the pool)
        coEvery { ownershipService.get(shortOwnership.id) } returns shortOwnership
        // Means order is cancelled
        coEvery {
            bestOrderService.updateBestSellOrder(shortOwnership, hackedOrder, emptyList())
        } returns expectedShortOwnership

        coEvery { ownershipService.save(expectedShortOwnership) } returns expectedShortOwnership

        coEvery {
            ownershipService.enrichOwnership(
                expectedShortOwnership,
                null,
                listOf(hackedOrder).associateBy { it.id })
        } returns OwnershipDtoConverter.convert(randomUnionOwnership())

        ownershipEventService.onPoolOrderUpdated(shortOwnership.id, order, PoolItemAction.EXCLUDED, stubEventMark())

        // Listener should be notified, Ownership - updated and Item data should be recalculated
        coVerify(exactly = 1) { eventListener.onEvent(any()) }
        coVerify(exactly = 1) { ownershipService.save(expectedShortOwnership) }
        coVerify(exactly = 1) {
            itemEventService.onOwnershipUpdated(
                oldOwnership = shortOwnership,
                newOwnership = expectedShortOwnership,
                order = hackedOrder,
                eventTimeMarks = any()
            )
        }
        coVerify(exactly = 0) { ownershipService.delete(shortOwnership.id) }
        coVerify(exactly = 0) { reconciliationEventService.onCorruptedOwnership(any()) }
    }

    @Test
    fun `on ownership best sell order updated - ownership exist, order not updated`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val temp = randomShortOwnership(itemId)
        val order = randomUnionSellOrder(itemId, temp.id.owner)
        val shortOrder = ShortOrderConverter.convert(order)
        val ownership = temp.copy(bestSellOrder = shortOrder)

        // Ownership exists, best Order is the same - nothing should happen here
        coEvery { ownershipService.get(ownership.id) } returns ownership
        coEvery { bestOrderService.updateBestSellOrder(ownership, order, emptyList()) } returns ownership

        ownershipEventService.onOwnershipBestSellOrderUpdated(ownership.id, order, stubEventMark())

        // Since nothing changed for Ownership, and it's order, we should skip such update
        coVerify(exactly = 0) { eventListener.onEvent(any()) }
        coVerify(exactly = 0) { ownershipService.save(any()) }
        coVerify(exactly = 0) {
            itemEventService.onOwnershipUpdated(
                oldOwnership = ownership,
                newOwnership = ownership,
                order = order,
                eventTimeMarks = any()
            )
        }
        coVerify(exactly = 0) { ownershipService.delete(ownership.id) }
        coVerify(exactly = 0) { reconciliationEventService.onCorruptedOwnership(any()) }
    }

    @Test
    fun `on ownership deleted - success`() = runBlocking {
        val ownershipId = ShortOwnershipId(randomEthOwnershipId())
        val deletedOwnership = randomShortOwnership()

        coEvery { ownershipService.delete(ownershipId) } returns deletedOwnership

        ownershipEventService.onOwnershipDeleted(UnionOwnershipDeleteEvent(ownershipId.toDto(), stubEventMark()))

        // Ownership deleted, listeners notified, item recalculated
        coVerify(exactly = 1) { eventListener.onEvent(any()) }
        coVerify(exactly = 1) { ownershipService.delete(ownershipId) }
        coVerify(exactly = 1) {
            itemEventService.onOwnershipUpdated(
                oldOwnership = deletedOwnership,
                newOwnership = null,
                order = null,
                eventTimeMarks = any()
            )
        }
    }

    @Test
    fun `on ownership deleted - nothing to delete`() = runBlocking {
        val ownershipId = ShortOwnershipId(randomEthOwnershipId())

        coEvery { ownershipService.delete(ownershipId) } returns null

        ownershipEventService.onOwnershipDeleted(UnionOwnershipDeleteEvent(ownershipId.toDto(), stubEventMark()))

        // Even we don't have Ownership in DB, we need to notify listeners, but we should not recalculate Item sell stat
        coVerify(exactly = 1) { eventListener.onEvent(any()) }
        coVerify(exactly = 1) { ownershipService.delete(ownershipId) }
        coVerify(exactly = 0) {
            itemEventService.onOwnershipUpdated(
                oldOwnership = any(),
                newOwnership = any(),
                order = any(),
                any()
            )
        }
    }

    @Test
    fun `on activity - source or ownershipId not extracted`() = runBlocking<Unit> {
        val burn = randomUnionActivityBurn(randomEthItemId())
        val emptySource = randomUnionActivityTransfer(randomEthItemId()).copy(purchase = null)

        ownershipEventService.onActivity(burn, null, stubEventMark())
        ownershipEventService.onActivity(emptySource, null, stubEventMark())

        coVerify(exactly = 0) { ownershipService.get(any()) }
    }

    @Test
    fun `on activity - source updated`() = runBlocking<Unit> {
        val mint = randomUnionActivityMint(randomEthItemId())
        val ownershipId = mint.ownershipId()!!

        val shortOwnership = randomShortOwnership(ownershipId).copy(source = OwnershipSourceDto.TRANSFER)

        coEvery { ownershipService.getOrEmpty(ShortOwnershipId(ownershipId)) } returns shortOwnership
        coEvery { ownershipService.save(any()) } returnsArgument 1

        ownershipEventService.onActivity(mint, null, stubEventMark(), false)

        coVerify(exactly = 1) { ownershipService.save(shortOwnership.copy(source = OwnershipSourceDto.MINT)) }
    }

    @Test
    fun `on activity - source is the same`() = runBlocking<Unit> {
        val mint = randomUnionActivityMint(randomEthItemId())
        val ownershipId = mint.ownershipId()!!

        val shortOwnership = randomShortOwnership(ownershipId).copy(source = OwnershipSourceDto.MINT)

        coEvery { ownershipService.getOrEmpty(ShortOwnershipId(ownershipId)) } returns shortOwnership

        ownershipEventService.onActivity(mint, null, stubEventMark(), false)

        coVerify(exactly = 0) { ownershipService.save(any()) }
    }

    @Test
    fun `on activity - source is not preferred`() = runBlocking<Unit> {
        val mint = randomUnionActivityTransfer(randomEthItemId())
        val ownershipId = mint.ownershipId()!!

        val shortOwnership = randomShortOwnership(ownershipId).copy(source = OwnershipSourceDto.MINT)

        coEvery { ownershipService.getOrEmpty(ShortOwnershipId(ownershipId)) } returns shortOwnership

        ownershipEventService.onActivity(mint, null, stubEventMark(), false)

        coVerify(exactly = 0) { ownershipService.save(any()) }
    }

    @Test
    fun `on reverted activity - source changed`() = runBlocking<Unit> {
        val mint = randomUnionActivityMint(randomEthItemId()).copy(reverted = true)
        val ownershipId = mint.ownershipId()!!

        val shortOwnership = randomShortOwnership(ownershipId).copy(source = OwnershipSourceDto.MINT)

        coEvery { ownershipService.getOrEmpty(ShortOwnershipId(ownershipId)) } returns shortOwnership
        coEvery { activityService.getOwnershipSource(ownershipId) } returns OwnershipSourceDto.TRANSFER
        coEvery { ownershipService.save(any()) } returnsArgument 1

        ownershipEventService.onActivity(mint, null, stubEventMark(), false)

        coVerify(exactly = 1) { ownershipService.save(shortOwnership.copy(source = OwnershipSourceDto.TRANSFER)) }
    }

    @Test
    fun `on reverted activity - source not changed`() = runBlocking<Unit> {
        // We have reverted MINT, but current already is TRANSFER - nothing should be changed
        val mint = randomUnionActivityMint(randomEthItemId()).copy(reverted = true)
        val ownershipId = mint.ownershipId()!!

        val shortOwnership = randomShortOwnership(ownershipId).copy(source = OwnershipSourceDto.TRANSFER)

        coEvery { ownershipService.getOrEmpty(ShortOwnershipId(ownershipId)) } returns shortOwnership

        ownershipEventService.onActivity(mint, null, stubEventMark(), false)

        coVerify(exactly = 0) { ownershipService.save(any()) }
    }
}
