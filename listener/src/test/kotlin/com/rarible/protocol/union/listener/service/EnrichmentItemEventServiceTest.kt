package com.rarible.protocol.union.listener.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.event.OutgoingItemEventListener
import com.rarible.protocol.union.core.model.itemId
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.enrichment.converter.ItemLastSaleConverter
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.BestOrderService
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityService
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityBurn
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivitySale
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EnrichmentItemEventServiceTest {

    private val itemService: EnrichmentItemService = mockk()
    private val ownershipService: EnrichmentOwnershipService = mockk()
    private val activityService: EnrichmentActivityService = mockk()
    private val eventListener: OutgoingItemEventListener = mockk()
    private val itemRepository: ItemRepository = mockk()

    private val bestOrderService: BestOrderService = mockk()
    private val reconciliationEventService: ReconciliationEventService = mockk()

    private val itemEventService = EnrichmentItemEventService(
        itemService,
        ownershipService,
        activityService,
        listOf(eventListener),
        bestOrderService,
        reconciliationEventService,
        itemRepository,
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(
            itemService,
            ownershipService,
            eventListener,
            bestOrderService,
            activityService,
            reconciliationEventService
        )
        coEvery { eventListener.onEvent(any()) } returns Unit
        coEvery { reconciliationEventService.onCorruptedOwnership(any()) } returns Unit
    }

    @Test
    fun `on activity - last sale not extracted`() = runBlocking<Unit> {
        val burn = randomUnionActivityBurn(randomEthItemId())

        itemEventService.onActivity(burn)

        coVerify(exactly = 0) { itemService.get(any()) }
    }

    @Test
    fun `on activity - null last sale updated`() = runBlocking<Unit> {
        val sell = randomUnionActivitySale(randomEthItemId())
        val lastSale = ItemLastSaleConverter.convert(sell)!!
        val itemId = sell.itemId()!!

        val shortItem = randomShortItem(itemId)

        coEvery { itemService.getOrEmpty(ShortItemId(itemId)) } returns shortItem
        coEvery { itemService.save(any()) } returnsArgument 1

        itemEventService.onActivity(sell, null, false)

        coVerify(exactly = 1) { itemService.save(shortItem.copy(lastSale = lastSale)) }
    }

    @Test
    fun `on activity - last sale updated`() = runBlocking<Unit> {
        val sell = randomUnionActivitySale(randomEthItemId())
        val lastSale = ItemLastSaleConverter.convert(sell)!!
        val currentLastSale = lastSale.copy(date = lastSale.date.minusSeconds(1))
        val itemId = sell.itemId()!!

        val shortItem = randomShortItem(itemId).copy(lastSale = currentLastSale)

        coEvery { itemService.getOrEmpty(ShortItemId(itemId)) } returns shortItem
        coEvery { itemService.save(any()) } returnsArgument 1

        itemEventService.onActivity(sell, null, false)

        coVerify(exactly = 1) { itemService.save(shortItem.copy(lastSale = lastSale)) }
    }

    @Test
    fun `on activity - last sale is the same`() = runBlocking<Unit> {
        val sell = randomUnionActivitySale(randomEthItemId())
        val lastSale = ItemLastSaleConverter.convert(sell)
        val itemId = sell.itemId()!!

        val shortItem = randomShortItem(itemId).copy(lastSale = lastSale)

        coEvery { itemService.getOrEmpty(ShortItemId(itemId)) } returns shortItem

        itemEventService.onActivity(sell, null, false)

        coVerify(exactly = 0) { itemService.save(any()) }
    }

    @Test
    fun `on activity - last sale not updated`() = runBlocking<Unit> {
        val sell = randomUnionActivitySale(randomEthItemId())
        val lastSale = ItemLastSaleConverter.convert(sell)!!
        val currentLastSale = lastSale.copy(date = lastSale.date.plusSeconds(1))
        val itemId = sell.itemId()!!

        val shortItem = randomShortItem(itemId).copy(lastSale = currentLastSale)

        coEvery { itemService.getOrEmpty(ShortItemId(itemId)) } returns shortItem

        itemEventService.onActivity(sell, null, false)

        coVerify(exactly = 0) { itemService.save(any()) }
    }

    @Test
    fun `on reverted activity - last sale changed`() = runBlocking<Unit> {
        val sell = randomUnionActivitySale(randomEthItemId()).copy(reverted = true)
        val lastSale = ItemLastSaleConverter.convert(sell)!!
        val actualLastSale = lastSale.copy(date = nowMillis().plusSeconds(5))
        val itemId = sell.itemId()!!

        val shortItem = randomShortItem(itemId).copy(lastSale = lastSale)

        coEvery { itemService.getOrEmpty(ShortItemId(itemId)) } returns shortItem
        coEvery { activityService.getItemLastSale(itemId) } returns actualLastSale
        coEvery { itemService.save(any()) } returnsArgument 1

        itemEventService.onActivity(sell, null, false)

        coVerify(exactly = 1) { itemService.save(shortItem.copy(lastSale = actualLastSale)) }
    }

    @Test
    fun `on reverted activity - source not changed`() = runBlocking<Unit> {
        val sell = randomUnionActivitySale(randomEthItemId()).copy(reverted = true)
        val lastSale = ItemLastSaleConverter.convert(sell)!!

        // Current last sale is different, should not be updated
        val currentLastSale = lastSale.copy(date = nowMillis().plusSeconds(5))
        val itemId = sell.itemId()!!

        val shortItem = randomShortItem(itemId).copy(lastSale = currentLastSale)

        coEvery { itemService.getOrEmpty(ShortItemId(itemId)) } returns shortItem

        itemEventService.onActivity(sell, null, false)

        coVerify(exactly = 0) { itemService.save(any()) }
    }

}
