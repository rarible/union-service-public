package com.rarible.protocol.union.worker.job

import com.rarible.core.test.data.randomBigDecimal
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.EthCollectionAssetTypeDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionBidOrderDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReconciliationCorruptedItemJobTest {

    private val refreshService: EnrichmentRefreshService = mockk()
    private val orderService: OrderService = mockk()
    private val orderServiceRouter: BlockchainRouter<OrderService> = mockk()
    private val itemRepository: ItemRepository = mockk()

    private val job = ReconciliationCorruptedItemJob(
        itemRepository,
        orderServiceRouter,
        refreshService
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(refreshService, orderServiceRouter, itemRepository)
        coEvery { orderServiceRouter.getService(BlockchainDto.ETHEREUM) } returns orderService
        coEvery { refreshService.reconcileItem(any(), true) } returns mockk() // doesn't matter
    }

    @Test
    fun `reconcile corrupted items`() = runBlocking<Unit> {
        val correctItemId = randomEthItemId()
        val correctBid = randomUnionBidOrderDto(correctItemId)
        val correctSell = randomUnionSellOrderDto(correctItemId)
        val correctItem = randomShortItem(correctItemId).copy(
            bestSellOrder = ShortOrderConverter.convert(correctSell),
            bestBidOrder = ShortOrderConverter.convert(correctBid)
        )

        val corruptedItemId = randomEthItemId()
        val corruptedBid = randomUnionBidOrderDto(corruptedItemId).copy(status = OrderStatusDto.INACTIVE)
        val corruptedItem = randomShortItem(corruptedItemId).copy(
            bestSellOrder = null,
            bestBidOrder = ShortOrderConverter.convert(corruptedBid)
        )

        val missedOrderItemId = randomEthItemId()
        val missedOrderSell = randomUnionSellOrderDto(missedOrderItemId)
        val missedOrderItem = randomShortItem(missedOrderItemId).copy(
            bestSellOrder = ShortOrderConverter.convert(missedOrderSell),
            bestBidOrder = null
        )

        // First page
        coEvery {
            itemRepository.findByBlockchain(null, BlockchainDto.ETHEREUM, 100)
        } returns flowOf(correctItem, corruptedItem, missedOrderItem)

        // Second page
        coEvery {
            itemRepository.findByBlockchain(ShortItemId(missedOrderItemId), BlockchainDto.ETHEREUM, 100)
        } returns emptyFlow()

        coEvery {
            orderService.getOrdersByIds(any())
        } returns listOf(correctBid, correctSell, corruptedBid)

        job.reconcileCorruptedItems(null, BlockchainDto.ETHEREUM).toList()

        coVerify(exactly = 0) { refreshService.reconcileItem(correctItemId, true) }
        coVerify(exactly = 1) { refreshService.reconcileItem(corruptedItemId, true) }
        coVerify(exactly = 1) { refreshService.reconcileItem(missedOrderItemId, true) }
        coVerify(exactly = 1) { orderService.getOrdersByIds(any()) }
    }

    @Test
    fun `clenaup legacy floor bid`() = runBlocking<Unit> {

        val collectionAssetType = EthCollectionAssetTypeDto(ContractAddress(BlockchainDto.ETHEREUM, randomEthAddress()))
        val take = AssetDto(collectionAssetType, randomBigDecimal())

        val floorBidOrderItemId = randomEthItemId()
        val floorBidOrder = randomUnionBidOrderDto(floorBidOrderItemId).copy(take = take)
        val floorBidOrderItem = randomShortItem(floorBidOrderItemId).copy(
            bestSellOrder = null,
            bestBidOrder = ShortOrderConverter.convert(floorBidOrder)
        )

        coEvery {
            orderService.getOrdersByIds(any())
        } returns listOf(floorBidOrder)

        // First page
        coEvery {
            itemRepository.findByBlockchain(null, BlockchainDto.ETHEREUM, 100)
        } returns flowOf(floorBidOrderItem)

        // Second page
        coEvery {
            itemRepository.findByBlockchain(ShortItemId(floorBidOrderItemId), BlockchainDto.ETHEREUM, 100)
        } returns emptyFlow()

        job.reconcileCorruptedItems(null, BlockchainDto.ETHEREUM).toList()

        coVerify(exactly = 1) { refreshService.reconcileItem(floorBidOrderItemId, true) }
    }
}