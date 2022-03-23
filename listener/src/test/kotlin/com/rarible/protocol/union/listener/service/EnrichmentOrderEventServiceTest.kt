package com.rarible.protocol.union.listener.service

import com.rarible.core.test.data.randomBigDecimal
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.EthCollectionAssetTypeDto
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc1155
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc20
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc721
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
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
    private val enrichmentCollectionEventService: EnrichmentCollectionEventService = mockk()

    private val orderEventService = EnrichmentOrderEventService(
        enrichmentItemEventService,
        enrichmentOwnershipEventService,
        enrichmentCollectionEventService,
        emptyList(),
        FeatureFlagsProperties()
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(enrichmentItemEventService, enrichmentOwnershipEventService)
        coEvery { enrichmentItemEventService.onItemBestSellOrderUpdated(any(), any()) } returns Unit
        coEvery { enrichmentItemEventService.onItemBestBidOrderUpdated(any(), any()) } returns Unit
        coEvery { enrichmentOwnershipEventService.onOwnershipBestSellOrderUpdated(any(), any()) } returns Unit
        coEvery { enrichmentCollectionEventService.onCollectionBestSellOrderUpdate(any(), any(), any()) } returns Unit
        coEvery { enrichmentCollectionEventService.onCollectionBestBidOrderUpdate(any(), any(), any()) } returns Unit
    }

    @Test
    fun `best sell order update`() = runBlocking {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)

        val shortItemId = ShortItemId(itemId)
        val shortOwnershipId = ShortOwnershipId(ownershipId)

        val order = randomUnionSellOrderDto(itemId, ownershipId.owner.value)
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
    fun `best sell collection order update`() = runBlocking {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)

        val shortItemId = ShortItemId(itemId)
        val collectionId = randomEthCollectionId()
        val assetAddress = ContractAddressConverter.convert(itemId.blockchain, collectionId.value)

        val order = randomUnionSellOrderDto(itemId, ownershipId.owner.value)
            .copy(
                make = AssetDto(EthCollectionAssetTypeDto(assetAddress), randomBigDecimal()),
                take = EthConverter.convert(randomEthAssetErc20(), itemId.blockchain)
            )

        orderEventService.updateOrder(order)

        coVerify(exactly = 0) { enrichmentItemEventService.onItemBestSellOrderUpdated(shortItemId, order) }
        coVerify(exactly = 0) { enrichmentItemEventService.onItemBestBidOrderUpdated(any(), any()) }
        coVerify(exactly = 1) {
            enrichmentCollectionEventService.onCollectionBestSellOrderUpdate(collectionId, order, true)
        }
    }

    @Test
    fun `best bid collection order update`() = runBlocking {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)

        val shortItemId = ShortItemId(itemId)
        val collectionId = randomEthCollectionId()
        val assetAddress = ContractAddressConverter.convert(itemId.blockchain, collectionId.value)

        val order = randomUnionSellOrderDto(itemId, ownershipId.owner.value)
            .copy(
                make = EthConverter.convert(randomEthAssetErc20(), itemId.blockchain),
                take = AssetDto(EthCollectionAssetTypeDto(assetAddress), randomBigDecimal())
            )

        orderEventService.updateOrder(order)

        coVerify(exactly = 0) { enrichmentItemEventService.onItemBestSellOrderUpdated(shortItemId, order) }
        coVerify(exactly = 0) { enrichmentItemEventService.onItemBestBidOrderUpdated(any(), any()) }
        coVerify(exactly = 1) {
            enrichmentCollectionEventService.onCollectionBestBidOrderUpdate(collectionId, order, true)
        }
    }

    @Test
    fun `best bid order update`() = runBlocking {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)

        val shortItemId = ShortItemId(itemId)

        val order = randomUnionSellOrderDto(itemId, ownershipId.owner.value)
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
