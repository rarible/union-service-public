package com.rarible.protocol.union.api.controller.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.NftActivityFilterByItemDto
import com.rarible.protocol.dto.OrderActivityFilterByItemDto
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDeleteEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.OwnershipSourceDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.util.bidCurrencyId
import com.rarible.protocol.union.enrichment.util.sellCurrencyId
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc1155
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMintActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthLegacyBidOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthLegacySellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderActivityMatch
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Mono
import scalether.domain.Address

@IntegrationTest
@ExperimentalCoroutinesApi
class RefreshControllerFt : AbstractIntegrationTest() {

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @Autowired
    lateinit var ethAuctionConverter: EthAuctionConverter

    @Autowired
    lateinit var enrichmentItemService: EnrichmentItemService

    @Autowired
    lateinit var enrichmentOwnershipService: EnrichmentOwnershipService

    @Test
    fun `reconcile item - full`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId)
        val unionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val shortItem = ShortItemConverter.convert(unionItem)

        val ethBestSell = randomEthLegacySellOrderDto(ethItemId)
        val unionBestSell = ethOrderConverter.convert(ethBestSell, ethItemId.blockchain)
        val shortBestSell = ShortOrderConverter.convert(unionBestSell)

        val ethBestBid = randomEthLegacyBidOrderDto(ethItemId)
        val unionBestBid = ethOrderConverter.convert(ethBestBid, ethItemId.blockchain)
        val shortBestBid = ShortOrderConverter.convert(unionBestBid)

        val ethAuction = randomEthAuctionDto(ethItemId)
        val auction = ethAuctionConverter.convert(ethAuction, BlockchainDto.ETHEREUM)

        // Fully auctioned ownership, should not be saved, but disguised event is expected for it
        val ethAuctionedOwnershipId = ethItemId.toOwnership(auction.seller.value)
        val auctionOwnershipId = ethItemId.toOwnership(auction.contract.value)
        val auctionOwnership = randomEthOwnershipDto(auctionOwnershipId)

        // Free ownership - should be reconciled in regular way
        val ethFreeOwnershipId = ethItemId.toOwnership(auction.seller.value)
        val ethOwnership = randomEthOwnershipDto(ethFreeOwnershipId)
        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, ethFreeOwnershipId.blockchain)
        val shortOwnership = ShortOwnershipConverter.convert(unionOwnership)

        // Last sell activity for item
        val swapDto = randomEthOrderActivityMatch()
        val activity = swapDto.copy(left = swapDto.left.copy(asset = randomEthAssetErc1155(ethItemId)))

        ethereumOwnershipControllerApiMock.mockGetNftOwnershipsByItem(ethItemId, null, 1000, ethOwnership)
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(auctionOwnershipId, auctionOwnership)
        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(ethItemId, listOf(ethAuction))
        ethereumItemControllerApiMock.mockGetNftItemById(ethItemId, ethItem)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(ethItemId, ethBestSell.take.assetType)
        // Best sell for Item
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(
            ethItemId,
            unionBestSell.sellCurrencyId,
            ethBestSell
        )
        // Same best sell for free Ownership
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(
            ethFreeOwnershipId,
            unionBestSell.sellCurrencyId,
            ethBestSell
        )

        val mintActivity = randomEthItemMintActivity().copy(owner = Address.apply(ethFreeOwnershipId.owner.value))
        ethereumActivityControllerApiMock.mockGetNftActivitiesByItem(
            ethItemId,
            listOf(NftActivityFilterByItemDto.Types.MINT),
            1,
            null,
            ActivitySortDto.LATEST_FIRST,
            mintActivity
        )

        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(ethItemId, ethBestBid.make.assetType)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(
            ethItemId,
            unionBestBid.bidCurrencyId,
            ethBestBid
        )

        mockLastSellActivity(ethItemId, activity)

        val uri = "$baseUri/v0.1/refresh/item/${ethItemId.fullId()}/reconcile?full=true"
        val result = testRestTemplate.postForEntity(uri, null, ItemEventDto::class.java).body!!
        val reconciled = (result as ItemUpdateEventDto).item
        val savedShortItem = enrichmentItemService.get(shortItem.id)!!
        val savedShortOwnership = enrichmentOwnershipService.get(shortOwnership.id)!!

        assertThat(savedShortItem.bestSellOrder!!.id).isEqualTo(shortBestSell.id)
        assertThat(savedShortItem.bestBidOrder!!.id).isEqualTo(shortBestBid.id)
        assertThat(savedShortItem.bestSellOrders[unionBestSell.sellCurrencyId]!!.id).isEqualTo(shortBestSell.id)
        assertThat(savedShortItem.bestBidOrders[unionBestBid.bidCurrencyId]!!.id).isEqualTo(shortBestBid.id)
        assertThat(savedShortItem.auctions).isEqualTo(setOf(auction.id))
        assertThat(savedShortItem.lastSale!!.date).isEqualTo(activity.date)

        assertThat(savedShortOwnership.source).isEqualTo(OwnershipSourceDto.MINT)
        assertThat(savedShortOwnership.bestSellOrder!!.id).isEqualTo(shortBestSell.id)
        assertThat(savedShortOwnership.bestSellOrders[unionBestSell.sellCurrencyId]!!.id).isEqualTo(shortBestSell.id)

        assertThat(reconciled.bestSellOrder!!.id).isEqualTo(unionBestSell.id)
        assertThat(reconciled.bestBidOrder!!.id).isEqualTo(unionBestBid.id)

        coVerify {
            testItemEventProducer.send(match<KafkaMessage<ItemEventDto>> { message ->
                message.value is ItemUpdateEventDto && message.value.itemId == ethItemId
            })
        }
        coVerify(exactly = 1) {
            testOwnershipEventProducer.send(match<KafkaMessage<OwnershipEventDto>> { message ->
                val ownership = (message.value as OwnershipUpdateEventDto).ownership
                ownership.id == ethAuctionedOwnershipId && ownership.bestSellOrder!!.id == unionBestSell.id
            })
        }
        coVerify(exactly = 1) {
            testOwnershipEventProducer.send(match<KafkaMessage<OwnershipEventDto>> { message ->
                val ownership = (message.value as OwnershipUpdateEventDto).ownership
                ownership.id == ethAuctionedOwnershipId && ownership.auction == auction
            })
        }
    }

    @Test
    fun `reconcile ownership - partially auctioned`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()

        val ethAuction = randomEthAuctionDto(ethItemId)
        val auction = ethAuctionConverter.convert(ethAuction, BlockchainDto.ETHEREUM)

        val ethOwnershipId = ethItemId.toOwnership(auction.seller.value)
        val ethOwnership = randomEthOwnershipDto(ethOwnershipId)
        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, ethOwnershipId.blockchain)
        val shortOwnership = ShortOwnershipConverter.convert(unionOwnership)

        val ethBestSell = randomEthLegacySellOrderDto(ethItemId)
        val unionBestSell = ethOrderConverter.convert(ethBestSell, ethOwnershipId.blockchain)
        val shortBestSell = ShortOrderConverter.convert(unionBestSell)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(
            ethItemId,
            ethOwnershipId.owner.value,
            listOf(ethAuction)
        )
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(ethOwnershipId, ethOwnership)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(ethItemId, ethBestSell.take.assetType)
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(
            ethOwnershipId,
            unionBestSell.sellCurrencyId,
            ethBestSell
        )

        val mintActivity = randomEthItemMintActivity().copy(owner = Address.apply(ethOwnershipId.owner.value))
        ethereumActivityControllerApiMock.mockGetNftActivitiesByItem(
            ethItemId,
            listOf(NftActivityFilterByItemDto.Types.MINT),
            1,
            null,
            ActivitySortDto.LATEST_FIRST,
            mintActivity
        )

        val uri = "$baseUri/v0.1/refresh/ownership/${ethOwnershipId.fullId()}/reconcile"
        val result = testRestTemplate.postForEntity(uri, null, OwnershipEventDto::class.java).body!!
        val reconciled = (result as OwnershipUpdateEventDto).ownership
        val savedShortOwnership = enrichmentOwnershipService.get(shortOwnership.id)!!

        assertThat(savedShortOwnership.source).isEqualTo(OwnershipSourceDto.MINT)
        assertThat(savedShortOwnership.bestSellOrder!!.id).isEqualTo(shortBestSell.id)
        assertThat(savedShortOwnership.bestSellOrders[unionBestSell.sellCurrencyId]!!.id).isEqualTo(shortBestSell.id)

        assertThat(reconciled.bestSellOrder!!.id).isEqualTo(unionBestSell.id)
        assertThat(reconciled.auction).isEqualTo(auction)

        coVerify(exactly = 1) {
            testOwnershipEventProducer.send(match<KafkaMessage<OwnershipEventDto>> { message ->
                message.value is OwnershipUpdateEventDto && message.value.ownershipId == ethOwnershipId
            })
        }
    }

    @Test
    fun `reconcile ownership - fully auctioned`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()

        val ethAuction = randomEthAuctionDto(ethItemId)
        val auction = ethAuctionConverter.convert(ethAuction, BlockchainDto.ETHEREUM)

        val ethOwnershipId = ethItemId.toOwnership(auction.seller.value)
        val auctionOwnershipId = ethItemId.toOwnership(auction.contract.value)
        val auctionOwnership = randomEthOwnershipDto(auctionOwnershipId)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(
            ethItemId,
            ethOwnershipId.owner.value,
            listOf(ethAuction)
        )
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipByIdNotFound(ethOwnershipId)
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(auctionOwnershipId, auctionOwnership)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(ethItemId)

        val uri = "$baseUri/v0.1/refresh/ownership/${ethOwnershipId.fullId()}/reconcile"
        val result = testRestTemplate.postForEntity(uri, null, OwnershipEventDto::class.java).body!!
        val reconciled = (result as OwnershipUpdateEventDto).ownership

        // Nothing to save - there should not be enrichment data for fully-auctioned ownerships
        assertThat(enrichmentOwnershipService.get(ShortOwnershipId(auctionOwnershipId))).isNull()
        assertThat(enrichmentOwnershipService.get(ShortOwnershipId(ethOwnershipId))).isNull()

        assertThat(reconciled.auction).isEqualTo(auction)

        coVerify(exactly = 1) {
            testOwnershipEventProducer.send(match<KafkaMessage<OwnershipEventDto>> { message ->
                message.value is OwnershipUpdateEventDto && message.value.ownershipId == ethOwnershipId
            })
        }
    }

    @Test
    fun `reconcile deleted item`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId).copy(deleted = true)

        val ethBestSell = randomEthLegacySellOrderDto(ethItemId)
        val unionBestSell = ethOrderConverter.convert(ethBestSell, ethItemId.blockchain)

        val ethBestBid = randomEthLegacyBidOrderDto(ethItemId)
        val unionBestBid = ethOrderConverter.convert(ethBestBid, ethItemId.blockchain)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(ethItemId, emptyList())
        ethereumItemControllerApiMock.mockGetNftItemById(ethItemId, ethItem)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(ethItemId, ethBestSell.take.assetType)
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(
            ethItemId,
            unionBestSell.sellCurrencyId,
            ethBestSell
        )

        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(ethItemId, ethBestBid.make.assetType)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(
            ethItemId,
            unionBestBid.bidCurrencyId,
            ethBestBid
        )
        mockLastSellActivity(ethItemId, null)

        val uri = "$baseUri/v0.1/refresh/item/${ethItemId.fullId()}/reconcile"
        val result = testRestTemplate.postForEntity(uri, null, ItemEventDto::class.java).body!!
        assertThat(result).isInstanceOf(ItemDeleteEventDto::class.java)

        coVerify {
            testItemEventProducer.send(match<KafkaMessage<ItemEventDto>> { message ->
                message.value is ItemDeleteEventDto && message.value.itemId == ethItemId
            })
        }
    }

    @Test
    fun `should ignore best sell order with filled taker`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId)
        val unionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val shortItem = ShortItemConverter.convert(unionItem)

        val ethBestSell = randomEthLegacySellOrderDto(ethItemId).copy(taker = Address.ONE())
        val unionBestSell = ethOrderConverter.convert(ethBestSell, ethItemId.blockchain)

        val ethBestBid = randomEthLegacyBidOrderDto(ethItemId)
        val unionBestBid = ethOrderConverter.convert(ethBestBid, ethItemId.blockchain)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(ethItemId, emptyList())
        ethereumItemControllerApiMock.mockGetNftItemById(ethItemId, ethItem)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(ethItemId, ethBestSell.take.assetType)
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(
            ethItemId,
            unionBestSell.sellCurrencyId,
            ethBestSell
        )

        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(ethItemId, ethBestBid.make.assetType)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(
            ethItemId,
            unionBestBid.bidCurrencyId,
            ethBestBid
        )
        mockLastSellActivity(ethItemId, null)

        val uri = "$baseUri/v0.1/refresh/item/${ethItemId.fullId()}/reconcile"
        val result = testRestTemplate.postForEntity(uri, null, ItemEventDto::class.java).body!!
        val reconciled = (result as ItemUpdateEventDto).item
        assertThat(reconciled.bestSellOrder).isNull()
        assertThat(reconciled.bestBidOrder).isNotNull()

        val savedShortItem = enrichmentItemService.get(shortItem.id)!!
        assertThat(savedShortItem.bestSellOrder).isNull()
        assertThat(savedShortItem.bestSellOrders).isEmpty()

        coVerify {
            testItemEventProducer.send(match<KafkaMessage<ItemEventDto>> { message ->
                message.value is ItemUpdateEventDto && message.value.itemId == ethItemId
            })
        }
    }

    // TODO should be moved to EnrichmentOrderServiceTest
    @Test
    fun `should ignore best sell order with filled taker for the first time`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val (ethItemContract, ethItemTokenId) = CompositeItemIdParser.split(ethItemId.value)
        val ethItem = randomEthNftItemDto(ethItemId)
        val unionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val shortItem = ShortItemConverter.convert(unionItem)

        val ethBestSell = randomEthLegacySellOrderDto(ethItemId)
        val ethBestSellWithTaker = randomEthLegacySellOrderDto(ethItemId).copy(taker = Address.ONE())
        val unionBestSell = ethOrderConverter.convert(ethBestSell, ethItemId.blockchain)
        val shortBestSell = ShortOrderConverter.convert(unionBestSell)

        val ethBestBid = randomEthLegacyBidOrderDto(ethItemId)
        val unionBestBid = ethOrderConverter.convert(ethBestBid, ethItemId.blockchain)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(ethItemId, emptyList())
        ethereumItemControllerApiMock.mockGetNftItemById(ethItemId, ethItem)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(
            ethItemId,
            ethBestSellWithTaker.take.assetType
        )
        val continuation = "continuation"
        every {
            testEthereumOrderApi.getSellOrdersByItemAndByStatus(
                eq(ethItemContract),
                eq(ethItemTokenId.toString()),
                any(),
                any(),
                any(),
                isNull(),
                any(),
                any(),
                any()
            )
        } returns Mono.just(OrdersPaginationDto(listOf(ethBestSellWithTaker), continuation))

        every {
            testEthereumOrderApi.getSellOrdersByItemAndByStatus(
                eq(ethItemContract),
                eq(ethItemTokenId.toString()),
                any(),
                any(),
                any(),
                eq(continuation),
                any(),
                any(),
                any()
            )
        } returns Mono.just(OrdersPaginationDto(listOf(ethBestSell), continuation))

        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(ethItemId, ethBestBid.make.assetType)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(
            ethItemId,
            unionBestBid.bidCurrencyId,
            ethBestBid
        )
        mockLastSellActivity(ethItemId, null)

        val uri = "$baseUri/v0.1/refresh/item/${ethItemId.fullId()}/reconcile"
        val result = testRestTemplate.postForEntity(uri, null, ItemEventDto::class.java).body!!
        val reconciled = (result as ItemUpdateEventDto).item

        assertThat(reconciled.bestSellOrder!!.id).isEqualTo(unionBestSell.id)

        val savedShortItem = enrichmentItemService.get(shortItem.id)!!
        assertThat(savedShortItem.bestSellOrder!!.id).isEqualTo(shortBestSell.id)
        assertThat(savedShortItem.bestSellOrders[unionBestSell.sellCurrencyId]!!.id).isEqualTo(shortBestSell.id)


        coVerify {
            testItemEventProducer.send(match<KafkaMessage<ItemEventDto>> { message ->
                message.value is ItemUpdateEventDto && message.value.itemId == ethItemId
            })
        }
    }

    private fun mockLastSellActivity(itemId: ItemIdDto, activity: OrderActivityMatchDto?) {
        ethereumActivityControllerApiMock.mockGetOrderActivitiesByItem(
            itemId,
            listOf(OrderActivityFilterByItemDto.Types.MATCH),
            1,
            null,
            ActivitySortDto.LATEST_FIRST,
            activity
        )
    }
}
