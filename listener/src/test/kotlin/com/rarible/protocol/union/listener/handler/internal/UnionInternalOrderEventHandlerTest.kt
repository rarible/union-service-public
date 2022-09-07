package com.rarible.protocol.union.listener.handler.internal

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.PoolItemAction
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.core.model.UnionPoolNftUpdateEvent
import com.rarible.protocol.union.core.model.UnionPoolOrderUpdateEvent
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import com.rarible.protocol.union.enrichment.test.data.randomSudoSwapAmmDataV1Dto
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.listener.service.EnrichmentOrderEventService
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class UnionInternalOrderEventHandlerTest {

    private val orderEventService: EnrichmentOrderEventService = mockk()
    private val enrichmentOrderService: EnrichmentOrderService = mockk()
    private val enrichmentItemService: EnrichmentItemService = mockk()
    private val reconciliationEventService: ReconciliationEventService = mockk()
    private val incomingEventHandler: IncomingEventHandler<UnionOrderEvent> = mockk()
    private val ff: FeatureFlagsProperties = FeatureFlagsProperties().copy(enablePoolOrders = true)

    private val handler = UnionInternalOrderEventHandler(
        orderEventService,
        enrichmentOrderService,
        enrichmentItemService,
        reconciliationEventService,
        incomingEventHandler,
        ff
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(
            orderEventService,
            enrichmentItemService,
            enrichmentOrderService,
            incomingEventHandler,
            reconciliationEventService
        )
        coEvery { incomingEventHandler.onEvent(any()) } returns Unit
    }

    @Test
    fun `regular order update`() = runBlocking<Unit> {
        val order = randomUnionSellOrderDto()

        coEvery { enrichmentOrderService.getById(order.id) } returns order
        coEvery { orderEventService.updateOrder(order, true) } returns Unit

        handler.onEvent(UnionOrderUpdateEvent(order))

        coVerify(exactly = 1) { orderEventService.updateOrder(order, true) }
    }

    @Test
    fun `regular order update - failed`() = runBlocking<Unit> {
        val order = randomUnionSellOrderDto()

        coEvery { enrichmentOrderService.getById(order.id) } throws NullPointerException()
        coEvery { reconciliationEventService.onFailedOrder(order) } returns Unit

        assertThrows<NullPointerException> { handler.onEvent(UnionOrderUpdateEvent(order)) }

        coVerify(exactly = 1) { reconciliationEventService.onFailedOrder(order) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["INCLUDED", "EXCLUDED"])
    fun `pool order internal event`(actionString: String) = runBlocking<Unit> {
        val action = PoolItemAction.valueOf(actionString)
        val order = randomUnionSellOrderDto()
        val itemId = randomEthItemId()
        coEvery { orderEventService.updatePoolOrder(order, itemId, action) } returns Unit

        handler.onEvent(UnionPoolOrderUpdateEvent(order, itemId, action))

        coVerify(exactly = 1) { orderEventService.updatePoolOrder(order, itemId, action) }
    }

    @Test
    fun `pool order internal event - failed`() = runBlocking<Unit> {
        val action = PoolItemAction.INCLUDED
        val order = randomUnionSellOrderDto()
        val itemId = randomEthItemId()

        coEvery { orderEventService.updatePoolOrder(order, itemId, action) } throws NullPointerException()
        coEvery { reconciliationEventService.onFailedOrder(order) } returns Unit

        assertThrows<NullPointerException> { handler.onEvent(UnionPoolOrderUpdateEvent(order, itemId, action)) }

        coVerify(exactly = 1) { reconciliationEventService.onFailedOrder(order) }
    }

    @Test
    fun `pool order update`() = runBlocking<Unit> {
        val order = randomUnionSellOrderDto().copy(data = randomSudoSwapAmmDataV1Dto())

        val itemId1 = ShortItemId(randomEthItemId())
        val itemId2 = ShortItemId(randomEthItemId())

        coEvery { enrichmentItemService.findByPoolOrder(order.id) } returns setOf(itemId1, itemId2)
        coEvery { enrichmentOrderService.getById(order.id) } returns order

        handler.onEvent(UnionOrderUpdateEvent(order))

        // 2 events sent for items from this pool
        coVerify(exactly = 1) {
            incomingEventHandler.onEvent(UnionPoolOrderUpdateEvent(order, itemId1.toDto(), PoolItemAction.INCLUDED))
        }
        coVerify(exactly = 1) {
            incomingEventHandler.onEvent(UnionPoolOrderUpdateEvent(order, itemId2.toDto(), PoolItemAction.INCLUDED))
        }
    }

    @Test
    fun `pool nft update`() = runBlocking<Unit> {
        val order = randomUnionSellOrderDto().copy(data = randomSudoSwapAmmDataV1Dto())

        val itemId1 = ShortItemId(randomEthItemId())
        val itemId2 = ShortItemId(randomEthItemId())
        val itemId3 = ShortItemId(randomEthItemId())
        val itemId4 = ShortItemId(randomEthItemId())
        val itemId5 = ShortItemId(randomEthItemId())

        // first already exists, second is new
        val included = setOf(itemId1.toDto(), itemId4.toDto())
        // first is excluded, second is not present in union
        val excluded = setOf(itemId2.toDto(), itemId5.toDto())

        coEvery { enrichmentItemService.findByPoolOrder(order.id) } returns setOf(itemId1, itemId2, itemId3)
        coEvery { enrichmentOrderService.getById(order.id) } returns order

        handler.onEvent(UnionPoolNftUpdateEvent(order.id, included, excluded))

        coVerify(exactly = 5) { incomingEventHandler.onEvent(any()) }

        val expectedIncluded = listOf(itemId1, itemId3, itemId4)
        val expectedExcluded = listOf(itemId2, itemId5)

        expectedIncluded.forEach {
            coVerify {
                incomingEventHandler.onEvent(UnionPoolOrderUpdateEvent(order, it.toDto(), PoolItemAction.INCLUDED))
            }
        }
        expectedExcluded.forEach {
            coVerify(exactly = 1) {
                incomingEventHandler.onEvent(UnionPoolOrderUpdateEvent(order, it.toDto(), PoolItemAction.EXCLUDED))
            }
        }
    }

}