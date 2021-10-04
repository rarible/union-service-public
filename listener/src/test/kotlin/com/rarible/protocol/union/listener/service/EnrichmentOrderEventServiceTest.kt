package com.rarible.protocol.union.listener.service

import com.rarible.protocol.union.core.ethereum.converter.EthConverter
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.test.data.randomEthAssetErc1155
import com.rarible.protocol.union.test.data.randomEthAssetErc20
import com.rarible.protocol.union.test.data.randomEthAssetErc721
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

class EnrichmentOrderEventServiceTest {

    private val enrichmentItemEventService: EnrichmentItemEventService = mockk()
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService = mockk()

    private val orderEventService = EnrichmentOrderEventService(
        enrichmentItemEventService,
        enrichmentOwnershipEventService
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(enrichmentItemEventService, enrichmentOwnershipEventService)
        coEvery { enrichmentItemEventService.onItemBestSellOrderUpdated(any(), any()) } returns Unit
        coEvery { enrichmentItemEventService.onItemBestBidOrderUpdated(any(), any()) } returns Unit
        coEvery { enrichmentOwnershipEventService.onOwnershipBestSellOrderUpdated(any(), any()) } returns Unit
    }

    @Test
    fun `best sell order update`() = runBlocking {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)

        val shortItemId = ShortItemId(itemId)
        val shortOwnershipId = ShortOwnershipId(ownershipId)

        val order = randomUnionOrderDto(itemId, ownershipId.owner.value)
            .copy(
                make = EthConverter.convert(randomEthAssetErc1155(itemId), itemId.blockchain),
                take = EthConverter.convert(randomEthAssetErc20(), itemId.blockchain)
            )

        orderEventService.updateOrder(order)

        coVerify(exactly = 1) { enrichmentItemEventService.onItemBestSellOrderUpdated(shortItemId, order) }
        coVerify(exactly = 0) { enrichmentItemEventService.onItemBestBidOrderUpdated(any(), any()) }
        coVerify(exactly = 1) {
            enrichmentOwnershipEventService.onOwnershipBestSellOrderUpdated(shortOwnershipId, order)
        }
    }

    @Test
    fun `best bid order update`() = runBlocking {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)

        val shortItemId = ShortItemId(itemId)

        val order = randomUnionOrderDto(itemId, ownershipId.owner.value)
            .copy(
                make = EthConverter.convert(randomEthAssetErc20(), itemId.blockchain),
                take = EthConverter.convert(randomEthAssetErc721(itemId), itemId.blockchain)
            )

        orderEventService.updateOrder(order)

        coVerify(exactly = 0) { enrichmentItemEventService.onItemBestSellOrderUpdated(any(), any()) }
        coVerify(exactly = 1) { enrichmentItemEventService.onItemBestBidOrderUpdated(shortItemId, order) }
        coVerify(exactly = 0) {
            enrichmentOwnershipEventService.onOwnershipBestSellOrderUpdated(any(), any())
        }
    }
}