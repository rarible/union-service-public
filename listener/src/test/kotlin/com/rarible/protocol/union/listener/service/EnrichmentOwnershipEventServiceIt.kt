package com.rarible.protocol.union.listener.service

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.union.core.model.ReconciliationMarkType
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.core.model.getAuctionOwnershipId
import com.rarible.protocol.union.core.model.getSellerOwnershipId
import com.rarible.protocol.union.core.model.stubEventMark
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.converter.OwnershipDtoConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.repository.ReconciliationMarkRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.enrichment.test.data.randomUnionAuctionDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionOwnership
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
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
    private lateinit var reconciliationMarkRepository: ReconciliationMarkRepository

    @Test
    fun `update event - ownership doesn't exist`() = runWithKafka {
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)
        val ownershipDto = randomUnionOwnership(ownershipId)

        val expected = OwnershipDtoConverter.convert(ownershipDto)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(itemId, emptyList())

        ownershipEventHandler.onOwnershipUpdated(UnionOwnershipUpdateEvent(ownershipDto, stubEventMark()))

        val created = ownershipService.get(ShortOwnershipId(ownershipId))!!
        // Ownership should not be updated since it wasn't in DB before update
        assertThat(created).isEqualTo(
            ShortOwnership.empty(created.id).copy(lastUpdatedAt = created.lastUpdatedAt, version = 0)
        )

        // But there should be single Ownership event "as is"
        waitAssert {
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

        val bestSellOrder = randomEthSellOrderDto(itemId)
        val unionBestSell = ethOrderConverter.convert(bestSellOrder, itemId.blockchain)

        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, itemId.blockchain)
        val shortOwnership = ShortOwnershipConverter.convert(unionOwnership).copy(
            bestSellOrder = ShortOrderConverter.convert(unionBestSell)
        )

        ownershipService.save(shortOwnership)

        ethereumOrderControllerApiMock.mockGetByIds(bestSellOrder)
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, ethOwnership)
        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(itemId, emptyList())

        ownershipEventHandler.onOwnershipUpdated(UnionOwnershipUpdateEvent(unionOwnership, stubEventMark()))

        val saved = ownershipService.get(shortOwnership.id)!!
        assertThat(saved.bestSellOrder).isEqualTo(shortOwnership.bestSellOrder)

        val expected = OwnershipDtoConverter.convert(unionOwnership)
            .copy(bestSellOrder = unionBestSell)

        // We don't have related item in enrichment DB, so expect only Ownership update
        waitAssert {
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
        val bestSellOrder = randomEthSellOrderDto(itemId).copy(status = OrderStatusDto.INACTIVE)
        val unionBestSell = ethOrderConverter.convert(bestSellOrder, itemId.blockchain)

        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, itemId.blockchain)
        val shortOwnership = ShortOwnershipConverter.convert(unionOwnership).copy(
            bestSellOrder = ShortOrderConverter.convert(unionBestSell)
        )

        ownershipService.save(shortOwnership)

        ethereumOrderControllerApiMock.mockGetByIds(bestSellOrder)
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, ethOwnership)
        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(itemId, emptyList())

        ownershipEventHandler.onOwnershipUpdated(UnionOwnershipUpdateEvent(unionOwnership, stubEventMark()))


        waitAssert {
            // Event should not be sent in case of corrupted enrichment data
            val messages = findOwnershipUpdates(ownershipId.value)
            assertThat(messages).hasSize(0)

            // Reconciliation mark should be created for such ownership
            val reconcileMarks = reconciliationMarkRepository.findByType(ReconciliationMarkType.OWNERSHIP, 100)
            val expectedMark = reconcileMarks.find { it.id == ownershipId.fullId() }
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

        val bestSellOrder = randomEthSellOrderDto(itemId)
        val unionBestSell = ethOrderConverter.convert(bestSellOrder, itemId.blockchain)

        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, ethOwnership)
        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(itemId, emptyList())

        ownershipEventHandler.onOwnershipBestSellOrderUpdated(shortOwnership.id, unionBestSell, stubEventMark())

        val saved = ownershipService.get(shortOwnership.id)!!
        assertThat(saved.bestSellOrder).isEqualTo(ShortOrderConverter.convert(unionBestSell))

        val expected = OwnershipDtoConverter.convert(unionOwnership)
            .copy(bestSellOrder = unionBestSell)

        // Since Item doesn't exist in Enrichment DB, we expect only Ownership event
        waitAssert {
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

        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, ethOwnership)

        ownershipEventHandler.onAuctionUpdated(auction)

        val expected = OwnershipDtoConverter.convert(unionOwnership)
            .copy(auction = auction)
            .copy(value = unionOwnership.value + auction.sell.value.toBigInteger())

        // Auction should be attached to the Ownership
        waitAssert {
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

        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(ownershipId, ethOwnership)
        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(itemId, emptyList())

        ownershipEventHandler.onAuctionUpdated(auction)

        val expected = OwnershipDtoConverter.convert(unionOwnership)

        // Auction should be NOT attached to the Ownership since it is cancelled
        waitAssert {
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

        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(unionAuctionOwnership.id, auctionOwnership)
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipByIdNotFound(ownershipId)

        ownershipEventHandler.onAuctionUpdated(auction)

        // Auction should be converted to disguised ownership
        waitAssert {
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

        ethereumOwnershipControllerApiMock.mockGetNftOwnershipByIdNotFound(ownershipId)

        ownershipEventHandler.onAuctionUpdated(auction)

        // Seller's ownership should be deleted
        waitAssert {
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

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(itemId, emptyList())

        ownershipEventHandler.onOwnershipDeleted(UnionOwnershipDeleteEvent(ownershipId, stubEventMark()))

        assertThat(ownershipService.get(ownership.id)).isNull()
        waitAssert {
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

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(itemId, emptyList())

        ownershipEventHandler.onOwnershipDeleted(UnionOwnershipDeleteEvent(ownershipId, stubEventMark()))

        assertThat(ownershipService.get(shortOwnershipId)).isNull()

        waitAssert {
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
        val auctionOwnershipId = itemId.toOwnership(unionAuction.contract.value)
        val auctionOwnership = randomEthOwnershipDto(auctionOwnershipId)

        val ownershipId = itemId.toOwnership(unionAuction.seller.value)
        val shortOwnershipId = ShortOwnershipId(ownershipId)

        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(auctionOwnershipId, auctionOwnership)
        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(itemId, listOf(auction))

        ownershipEventHandler.onOwnershipDeleted(UnionOwnershipDeleteEvent(ownershipId, stubEventMark()))

        assertThat(ownershipService.get(shortOwnershipId)).isNull()

        waitAssert {
            val messages = findOwnershipUpdates(ownershipId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.ownership.id).isEqualTo(ownershipId)
            assertThat(messages[0].value.ownership.auction).isEqualTo(unionAuction)
        }
    }
}
