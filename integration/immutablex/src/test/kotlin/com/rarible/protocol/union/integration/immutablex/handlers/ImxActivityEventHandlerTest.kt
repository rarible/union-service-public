package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.integration.data.randomImxMint
import com.rarible.protocol.union.integration.data.randomImxOrder
import com.rarible.protocol.union.integration.data.randomImxOrderBuySide
import com.rarible.protocol.union.integration.data.randomImxOrderSellSide
import com.rarible.protocol.union.integration.data.randomImxTrade
import com.rarible.protocol.union.integration.data.randomImxTransfer
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfer
import com.rarible.protocol.union.integration.immutablex.converter.ImxActivityConverter
import com.rarible.protocol.union.integration.immutablex.converter.ImxItemConverter
import com.rarible.protocol.union.integration.immutablex.converter.ImxOwnershipConverter
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanMetrics
import com.rarible.protocol.union.integration.immutablex.service.ImxActivityService
import com.rarible.protocol.union.integration.immutablex.service.ImxItemService
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImxActivityEventHandlerTest {

    private val activityHandler: IncomingEventHandler<ActivityDto> = mockk {
        coEvery { onEvent(any()) } returns Unit
    }
    private val itemHandler: IncomingEventHandler<UnionItemEvent> = mockk {
        coEvery { onEvent(any()) } returns Unit
    }
    private val ownershipHandler: IncomingEventHandler<UnionOwnershipEvent> = mockk {
        coEvery { onEvent(any()) } returns Unit
    }

    private val itemService: ImxItemService = mockk()
    private val activityService: ImxActivityService = mockk()

    private val imxScanMetrics: ImxScanMetrics = mockk()

    private val imxActivityEventHandler = ImxActivityEventHandler(
        activityHandler,
        itemHandler,
        ownershipHandler,
        itemService,
        activityService,
        imxScanMetrics
    )

    private val blockchain = BlockchainDto.IMMUTABLEX

    @BeforeEach
    fun beforeEach() {
        clearMocks(itemService, activityService)
    }

    @Test
    fun `on mint`() = runBlocking<Unit> {
        val mint = randomImxMint()

        val activity = ImxActivityConverter.convert(mint, emptyMap())
        val item = ImxItemConverter.convert(mint, blockchain)

        coEvery { activityService.getTradeOrders(listOf(mint)) } returns emptyMap()
        coEvery { itemService.getItemCreators(emptyList()) } returns emptyMap()

        imxActivityEventHandler.handle(listOf(mint))

        // Ony one specific activity sent
        coVerify(exactly = 1) { activityHandler.onEvent(any()) }
        coVerify(exactly = 1) { activityHandler.onEvent(activity) }

        // Item updated
        coVerify(exactly = 1) { itemHandler.onEvent(any()) }
        coVerify(exactly = 1) { itemHandler.onEvent(UnionItemUpdateEvent(item)) }

        // No ownership updates (BTW, maybe this is not correct)
        coVerify(exactly = 0) { ownershipHandler.onEvent(any()) }
    }

    @Test
    fun `on transfer`() = runBlocking<Unit> {
        val transfer = randomImxTransfer()
        val creator = randomAddress().prefixed()
        val user = UnionAddressConverter.convert(blockchain, transfer.user)
        val itemId = transfer.itemId()

        coEvery { activityService.getTradeOrders(listOf(transfer)) } returns emptyMap()
        coEvery { itemService.getItemCreators(listOf(itemId)) } returns mapOf(itemId to creator)

        val deletedOwnershipId = OwnershipIdDto(blockchain, itemId, user)
        val activity = ImxActivityConverter.convert(transfer, emptyMap())
        val ownership = ImxOwnershipConverter.convert(transfer, creator, blockchain)

        imxActivityEventHandler.handle(listOf(transfer))

        // Ony one specific activity sent
        coVerify(exactly = 1) { activityHandler.onEvent(any()) }
        coVerify(exactly = 1) { activityHandler.onEvent(activity) }

        // No item updates
        coVerify(exactly = 0) { itemHandler.onEvent(any()) }

        // Two ownership updates - deleted/updated
        coVerify(exactly = 2) { ownershipHandler.onEvent(any()) }
        coVerify(exactly = 1) { ownershipHandler.onEvent(UnionOwnershipDeleteEvent(deletedOwnershipId)) }
        coVerify(exactly = 1) { ownershipHandler.onEvent(UnionOwnershipUpdateEvent(ownership)) }
    }

    @Test
    fun `on burn`() = runBlocking<Unit> {
        val transfer = randomImxTransfer().copy(receiver = ImmutablexTransfer.ZERO_ADDRESS)
        val user = UnionAddressConverter.convert(blockchain, transfer.user)
        val itemId = transfer.itemId()

        coEvery { activityService.getTradeOrders(listOf(transfer)) } returns emptyMap()
        coEvery { itemService.getItemCreators(emptyList()) } returns emptyMap()

        val deletedOwnershipId = OwnershipIdDto(blockchain, itemId, user)
        val activity = ImxActivityConverter.convert(transfer, emptyMap())

        imxActivityEventHandler.handle(listOf(transfer))

        // Ony one specific activity sent
        coVerify(exactly = 1) { activityHandler.onEvent(any()) }
        coVerify(exactly = 1) { activityHandler.onEvent(activity) }

        // Item deleted
        coVerify(exactly = 1) { itemHandler.onEvent(UnionItemDeleteEvent(deletedOwnershipId.getItemId())) }

        // One ownership deleted, there is no new one ownership
        coVerify(exactly = 1) { ownershipHandler.onEvent(any()) }
        coVerify(exactly = 1) { ownershipHandler.onEvent(UnionOwnershipDeleteEvent(deletedOwnershipId)) }
    }

    @Test
    fun `on trade`() = runBlocking<Unit> {
        val sellOrder = randomImxOrder(sell = randomImxOrderSellSide(), buy = randomImxOrderBuySide())
        val buyOrder = randomImxOrder(sell = randomImxOrderBuySide(), buy = randomImxOrderSellSide())

        val trade = randomImxTrade(buyOrderId = buyOrder.orderId, sellOrderId = sellOrder.orderId)

        val orderMap = mapOf(sellOrder.orderId to sellOrder, buyOrder.orderId to buyOrder)
        val activity = ImxActivityConverter.convert(trade, orderMap)

        coEvery { itemService.getItemCreators(emptyList()) } returns emptyMap()
        coEvery { activityService.getTradeOrders(listOf(trade)) } returns orderMap

        imxActivityEventHandler.handle(listOf(trade))

        // Ony one specific activity sent
        coVerify(exactly = 1) { activityHandler.onEvent(any()) }
        coVerify(exactly = 1) { activityHandler.onEvent(activity) }

        // Nothing else emitted
        coVerify(exactly = 0) { itemHandler.onEvent(any()) }
        coVerify(exactly = 0) { ownershipHandler.onEvent(any()) }
    }
}
