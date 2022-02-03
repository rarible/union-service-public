package com.rarible.protocol.union.listener.service

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.AuctionsPaginationDto
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.union.core.model.getAuctionOwnershipId
import com.rarible.protocol.union.core.model.getSellerOwnershipId
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.converter.EnrichedOwnershipConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.repository.OwnershipReconciliationMarkRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.enrichment.test.data.randomUnionAuctionDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionOwnershipDto
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthLegacySellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.clearMocks
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.kotlin.core.publisher.toMono

@FlowPreview
@IntegrationTest
@Suppress("ReactiveStreamsUnusedPublisher")
class EnrichmentOwnershipEventServiceIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var ownershipEventHandler: EnrichmentOwnershipEventService

    @Autowired
    private lateinit var ownershipService: EnrichmentOwnershipService

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @Autowired
    lateinit var ethAuctionConverter: EthAuctionConverter

    @Autowired
    private lateinit var ownershipReconciliationMarkRepository: OwnershipReconciliationMarkRepository

    @BeforeEach
    fun beforeEach() {
        clearMocks(testEthereumAuctionApi)
    }

    @Test
    fun `update event - ownership doesn't exist`() = runWithKafka {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val ownershipDto = randomUnionOwnershipDto(ownershipId)

        val expected = EnrichedOwnershipConverter.convert(ownershipDto)

        coEvery {
            testEthereumAuctionApi.getAuctionsByItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), 1)
        } returns AuctionsPaginationDto(emptyList(), null).toMono()

        ownershipEventHandler.onOwnershipUpdated(ownershipDto)

        val created = ownershipService.get(ShortOwnershipId(ownershipId))
        // Ownership should not be updated since it wasn't in DB before update
        assertThat(created).isNull()

        // But there should be single Ownership event "as is"
        Wait.waitAssert {
            assertThat(ownershipEvents).hasSize(1)
            val messages = findOwnershipUpdates(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.ownershipId).isEqualTo(ownershipId)
            assertThat(messages[0].value.ownership).isEqualTo(expected)
        }
    }

    @Test
    fun `update event - existing ownership updated`() = runWithKafka {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val ethOwnership = randomEthOwnershipDto(ownershipId)

        val bestSellOrder = randomEthLegacySellOrderDto(itemId)
        val unionBestSell = ethOrderConverter.convert(bestSellOrder, itemId.blockchain)

        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, itemId.blockchain)
        val shortOwnership = ShortOwnershipConverter.convert(unionOwnership).copy(
            bestSellOrder = ShortOrderConverter.convert(unionBestSell)
        )

        ownershipService.save(shortOwnership)

        coEvery { testEthereumOrderApi.getOrderByHash(unionBestSell.id.value) } returns bestSellOrder.toMono()
        coEvery { testEthereumOwnershipApi.getNftOwnershipById(itemId.value) } returns ethOwnership.toMono()
        coEvery {
            testEthereumAuctionApi.getAuctionsByItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), 1)
        } returns AuctionsPaginationDto(emptyList(), null).toMono()

        ownershipEventHandler.onOwnershipUpdated(unionOwnership)

        val saved = ownershipService.get(shortOwnership.id)!!
        assertThat(saved.bestSellOrder).isEqualTo(shortOwnership.bestSellOrder)

        val expected = EnrichedOwnershipConverter.convert(unionOwnership)
            .copy(bestSellOrder = unionBestSell)

        // We don't have related item in enrichment DB, so expect only Ownership update
        Wait.waitAssert {
            assertThat(ownershipEvents).hasSize(1)
            val messages = findOwnershipUpdates(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.ownershipId).isEqualTo(ownershipId)
            assertThat(messages[0].value.ownership.bestSellOrder!!.id).isEqualTo(expected.bestSellOrder!!.id)
        }
    }

    @Test
    fun `update event - existing ownership updated, order corrupted`() = runWithKafka {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val ethOwnership = randomEthOwnershipDto(ownershipId)

        // Corrupted order with incorrect status
        val bestSellOrder = randomEthLegacySellOrderDto(itemId).copy(status = OrderStatusDto.INACTIVE)
        val unionBestSell = ethOrderConverter.convert(bestSellOrder, itemId.blockchain)

        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, itemId.blockchain)
        val shortOwnership = ShortOwnershipConverter.convert(unionOwnership).copy(
            bestSellOrder = ShortOrderConverter.convert(unionBestSell)
        )

        ownershipService.save(shortOwnership)

        coEvery { testEthereumOrderApi.getOrderByHash(unionBestSell.id.value) } returns bestSellOrder.toMono()
        coEvery { testEthereumOwnershipApi.getNftOwnershipById(itemId.value) } returns ethOwnership.toMono()
        coEvery {
            testEthereumAuctionApi.getAuctionsByItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), 1)
        } returns AuctionsPaginationDto(emptyList(), null).toMono()

        ownershipEventHandler.onOwnershipUpdated(unionOwnership)


        Wait.waitAssert {
            assertThat(ownershipEvents).hasSize(1)
            val messages = findOwnershipUpdates(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].id).isEqualTo(itemId.fullId())

            // Reconciliation mark should be created for such ownership
            val reconcileMarks = ownershipReconciliationMarkRepository.findAll(100)
            val expectedMark = reconcileMarks.find { it.id.toDto() == ownershipId }
            assertThat(expectedMark).isNotNull()
        }
    }

    @Test
    fun `on order updated - ownership exists`() = runWithKafka {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val ethOwnership = randomEthOwnershipDto(ownershipId)

        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, itemId.blockchain)
        val shortOwnership = ShortOwnershipConverter.convert(unionOwnership)
        ownershipService.save(shortOwnership)

        val bestSellOrder = randomEthLegacySellOrderDto(itemId)
        val unionBestSell = ethOrderConverter.convert(bestSellOrder, itemId.blockchain)

        coEvery { testEthereumOwnershipApi.getNftOwnershipById(ownershipId.value) } returns ethOwnership.toMono()
        coEvery {
            testEthereumAuctionApi.getAuctionsByItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), 1)
        } returns AuctionsPaginationDto(emptyList(), null).toMono()

        ownershipEventHandler.onOwnershipBestSellOrderUpdated(shortOwnership.id, unionBestSell)

        val saved = ownershipService.get(shortOwnership.id)!!
        assertThat(saved.bestSellOrder).isEqualTo(ShortOrderConverter.convert(unionBestSell))

        val expected = EnrichedOwnershipConverter.convert(unionOwnership)
            .copy(bestSellOrder = unionBestSell)

        // Since Item doesn't exist in Enrichment DB, we expect only Ownership event
        Wait.waitAssert {
            assertThat(ownershipEvents).hasSize(1)
            val messages = findOwnershipUpdates(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.ownershipId).isEqualTo(ownershipId)
            assertThat(messages[0].value.ownership.bestSellOrder!!.id).isEqualTo(expected.bestSellOrder!!.id)
        }
    }

    @Test
    fun `on auction updated - ownership fetched, auction is active`() = runWithKafka {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val ethOwnership = randomEthOwnershipDto(ownershipId)

        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, itemId.blockchain)

        val auction = randomUnionAuctionDto(ownershipId)

        coEvery { testEthereumOwnershipApi.getNftOwnershipById(ownershipId.value) } returns ethOwnership.toMono()

        ownershipEventHandler.onAuctionUpdated(auction)

        val expected = EnrichedOwnershipConverter.convert(unionOwnership)
            .copy(auction = auction)
            .copy(value = unionOwnership.value + auction.sell.value.toBigInteger())

        // Auction should be attached to the Ownership
        Wait.waitAssert {
            assertThat(ownershipEvents).hasSize(1)
            val messages = findOwnershipUpdates(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.ownership).isEqualTo(expected)
        }
    }

    @Test
    fun `on auction updated - ownership fetched, auction is cancelled`() = runWithKafka {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val ethOwnership = randomEthOwnershipDto(ownershipId)

        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, itemId.blockchain)

        val auction = randomUnionAuctionDto(ownershipId).copy(status = AuctionStatusDto.CANCELLED)

        coEvery { testEthereumOwnershipApi.getNftOwnershipById(ownershipId.value) } returns ethOwnership.toMono()
        coEvery {
            testEthereumAuctionApi.getAuctionsByItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), 1)
        } returns AuctionsPaginationDto(emptyList(), null).toMono()

        ownershipEventHandler.onAuctionUpdated(auction)

        val expected = EnrichedOwnershipConverter.convert(unionOwnership)

        // Auction should be NOT attached to the Ownership since it is cancelled
        Wait.waitAssert {
            assertThat(ownershipEvents).hasSize(1)
            val messages = findOwnershipUpdates(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.ownership).isEqualTo(expected)
        }
    }

    @Test
    fun `on auction updated - ownership not fetched, auction is active`() = runWithKafka {
        val itemId = randomEthItemId()
        val auction = randomUnionAuctionDto(itemId)
        val ownershipId = auction.getSellerOwnershipId()

        val auctionOwnership = randomEthOwnershipDto(auction.getAuctionOwnershipId())
        val unionAuctionOwnership = EthOwnershipConverter.convert(auctionOwnership, itemId.blockchain)

        coEvery {
            testEthereumOwnershipApi.getNftOwnershipById(unionAuctionOwnership.id.value)
        } returns auctionOwnership.toMono()

        coEvery {
            testEthereumOwnershipApi.getNftOwnershipById(ownershipId.value)
        } throws WebClientResponseException(404, "", null, null, null)

        ownershipEventHandler.onAuctionUpdated(auction)

        // Auction should be converted to disguised ownership
        Wait.waitAssert {
            assertThat(ownershipEvents).hasSize(1)
            val messages = findOwnershipUpdates(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.ownership.id).isEqualTo(ownershipId)
            assertThat(messages[0].value.ownership.auction).isEqualTo(auction)
        }
    }

    @Test
    fun `on auction updated - ownership not fetched, auction is finished`() = runWithKafka {
        val itemId = randomEthItemId()
        val auction = randomUnionAuctionDto(itemId).copy(status = AuctionStatusDto.FINISHED)
        val ownershipId = auction.getSellerOwnershipId()

        coEvery {
            testEthereumOwnershipApi.getNftOwnershipById(ownershipId.value)
        } throws WebClientResponseException(404, "", null, null, null)

        ownershipEventHandler.onAuctionUpdated(auction)

        // Seller's ownership should be deleted
        Wait.waitAssert {
            assertThat(ownershipEvents).hasSize(1)
            val messages = findOwnershipDeletions(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.ownershipId).isEqualTo(ownershipId)
        }
    }

    @Test
    fun `delete event - existing ownership deleted`() = runWithKafka {
        val itemId = randomEthItemId()
        val ownership = ownershipService.save(randomShortOwnership(itemId))
        val ownershipId = ownership.id.toDto()

        assertThat(ownershipService.get(ownership.id)).isNotNull()

        coEvery {
            testEthereumAuctionApi.getAuctionsByItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), 1)
        } returns AuctionsPaginationDto(emptyList(), null).toMono()

        ownershipEventHandler.onOwnershipDeleted(ownershipId)

        assertThat(ownershipService.get(ownership.id)).isNull()
        Wait.waitAssert {
            val messages = findOwnershipDeletions(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.ownershipId).isEqualTo(ownershipId)
        }
    }

    @Test
    fun `delete event - ownership doesn't exist`() = runWithKafka {
        val itemId = randomEthItemId()
        val shortOwnershipId = randomShortOwnership(itemId).id
        val ownershipId = shortOwnershipId.toDto()

        coEvery {
            testEthereumAuctionApi.getAuctionsByItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), 1)
        } returns AuctionsPaginationDto(emptyList(), null).toMono()

        ownershipEventHandler.onOwnershipDeleted(ownershipId)

        assertThat(ownershipService.get(shortOwnershipId)).isNull()

        Wait.waitAssert {
            val messages = findOwnershipDeletions(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.ownershipId).isEqualTo(ownershipId)
        }
    }

    @Test
    fun `delete event - fully auctioned ownership`() = runWithKafka {
        val itemId = randomEthItemId()
        val auction = randomEthAuctionDto(itemId)
        val unionAuction = ethAuctionConverter.convert(auction, BlockchainDto.ETHEREUM)
        val auctionOwnershipId = randomEthOwnershipId(itemId, unionAuction.contract.value)
        val auctionOwnership = randomEthOwnershipDto(auctionOwnershipId)

        val ownershipId = randomEthOwnershipId(itemId, unionAuction.seller.value)
        val shortOwnershipId = ShortOwnershipId(ownershipId)

        coEvery {
            testEthereumAuctionApi.getAuctionsByItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), 1)
        } returns AuctionsPaginationDto(listOf(auction), null).toMono()

        coEvery {
            testEthereumOwnershipApi.getNftOwnershipById(auctionOwnershipId.value)
        } returns auctionOwnership.toMono()

        ownershipEventHandler.onOwnershipDeleted(ownershipId)

        assertThat(ownershipService.get(shortOwnershipId)).isNull()

        Wait.waitAssert {
            val messages = findOwnershipUpdates(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.ownership.id).isEqualTo(ownershipId)
            assertThat(messages[0].value.ownership.auction).isEqualTo(unionAuction)
        }
    }
}
