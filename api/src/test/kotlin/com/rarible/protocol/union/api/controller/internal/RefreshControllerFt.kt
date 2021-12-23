package com.rarible.protocol.union.api.controller.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDeleteEventDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.util.bidCurrencyId
import com.rarible.protocol.union.enrichment.util.sellCurrencyId
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc20
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthLegacyBidOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthLegacySellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

@IntegrationTest
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
    fun `refresh item`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId)
        val unionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val shortItem = ShortItemConverter.convert(unionItem)

        // This order is not best sell anymore
        val makeAssetOutdated = randomEthAssetErc20()
        val ethBestSellOutdated = randomEthLegacySellOrderDto(ethItemId)
            .copy(make = makeAssetOutdated)
        val unionBestSellOutdated = ethOrderConverter.convert(ethBestSellOutdated, ethItemId.blockchain)
        val shortBestSellOutdated = ShortOrderConverter.convert(unionBestSellOutdated)

        // In the tests we're converting USD to currencies 1 to 1, so here we can just decrease make value
        val makeAsset = randomEthAssetErc20()
        val ethBestSell = randomEthLegacySellOrderDto(ethItemId)
            .copy(make = makeAsset.copy(valueDecimal = makeAssetOutdated.valueDecimal!!.minus(BigDecimal.ONE)))
        val unionBestSell = ethOrderConverter.convert(ethBestSell, ethItemId.blockchain)
        val shortBestSell = ShortOrderConverter.convert(unionBestSell)

        // Bid orders won't be changed
        val ethBestBid = randomEthLegacyBidOrderDto(ethItemId)
        val unionBestBid = ethOrderConverter.convert(ethBestBid, ethItemId.blockchain)
        val shortBestBid = ShortOrderConverter.convert(unionBestBid)
        val auctionDto = randomEthAuctionDto(ethItemId)

        enrichmentItemService.save(
            shortItem.copy(
                bestSellOrder = shortBestSellOutdated,
                bestBidOrder = shortBestBid,
                bestSellOrders = mapOf(
                    unionBestSellOutdated.sellCurrencyId to shortBestSellOutdated,
                    unionBestSell.sellCurrencyId to shortBestSell
                ),
                bestBidOrders = mapOf(unionBestBid.bidCurrencyId to shortBestBid)
            )
        )

        val uri = "$baseUri/v0.1/refresh/item/${ethItemId.fullId()}/refresh"

        ethereumItemControllerApiMock.mockGetNftItemById(ethItemId, ethItem)
        ethereumOrderControllerApiMock.mockGetById(ethBestSell, ethBestBid)
        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(ethItemId, listOf(auctionDto))
        ethereumAuctionControllerApiMock.mockGetAuctionsByIds(listOf(auctionDto.hash), listOf(auctionDto))

        val result = testRestTemplate.postForEntity(uri, null, ItemDto::class.java).body!!
        val savedShortItem = enrichmentItemService.get(shortItem.id)!!

        assertThat(savedShortItem.bestSellOrder).isEqualTo(shortBestSell)
        assertThat(savedShortItem.bestBidOrder).isEqualTo(shortBestBid)

        assertThat(result.bestSellOrder!!.id).isEqualTo(unionBestSell.id)
        assertThat(result.bestBidOrder!!.id).isEqualTo(unionBestBid.id)
        assertThat(result.auctions.size).isEqualTo(1)
        assertThat(result.auctions.first().id).isEqualTo(ethAuctionConverter.convert(auctionDto, BlockchainDto.ETHEREUM).id)

        coVerify {
            testItemEventProducer.send(match<KafkaMessage<ItemEventDto>> { message ->
                message.value.itemId == ethItemId
            })
        }
    }

    @Test
    fun `refresh deleted item`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId).copy(deleted = true)
        val unionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val shortItem = ShortItemConverter.convert(unionItem)

        enrichmentItemService.save(
            shortItem.copy(
                bestSellOrder = null,
                bestBidOrder = null,
                bestSellOrders = emptyMap(),
                bestBidOrders = emptyMap()
            )
        )

        val uri = "$baseUri/v0.1/refresh/item/${ethItemId.fullId()}/refresh"

        ethereumItemControllerApiMock.mockGetNftItemById(ethItemId, ethItem)
        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(ethItemId, emptyList())

        val result = testRestTemplate.postForEntity(uri, null, ItemDto::class.java).body!!

        assertThat(result.deleted).isTrue
        coVerify {
            testItemEventProducer.send(match<KafkaMessage<ItemEventDto>> { message ->
                message.value is ItemDeleteEventDto && message.value.itemId == ethItemId
            })
        }
    }

    @Test
    fun `refresh ownership`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val ethOwnershipId = randomEthOwnershipId(ethItemId)
        val ethOwnership = randomEthOwnershipDto(ethOwnershipId)
        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, ethOwnershipId.blockchain)
        val shortOwnership = ShortOwnershipConverter.convert(unionOwnership)

        // This order is not best sell anymore
        val makeAssetOutdated = randomEthAssetErc20()
        val ethBestSellOutdated = randomEthLegacySellOrderDto(ethItemId)
            .copy(make = makeAssetOutdated)
        val unionBestSellOutdated = ethOrderConverter.convert(ethBestSellOutdated, ethOwnershipId.blockchain)
        val shortBestSellOutdated = ShortOrderConverter.convert(unionBestSellOutdated)

        // In the tests we're converting USD to currencies 1 to 1, so here we can just decrease make value
        val makeAsset = randomEthAssetErc20()
        val ethBestSell = randomEthLegacySellOrderDto(ethItemId)
            .copy(make = makeAsset.copy(valueDecimal = makeAssetOutdated.valueDecimal!!.minus(BigDecimal.ONE)))
        val unionBestSell = ethOrderConverter.convert(ethBestSell, ethOwnershipId.blockchain)
        val shortBestSell = ShortOrderConverter.convert(unionBestSell)

        enrichmentOwnershipService.save(
            shortOwnership.copy(
                bestSellOrder = shortBestSellOutdated,
                bestSellOrders = mapOf(
                    unionBestSellOutdated.sellCurrencyId to shortBestSellOutdated,
                    unionBestSell.sellCurrencyId to shortBestSell
                )
            )
        )

        val uri = "$baseUri/v0.1/refresh/ownership/${ethOwnershipId.fullId()}/refresh"

        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(ethOwnershipId, ethOwnership)
        ethereumOrderControllerApiMock.mockGetById(ethBestSell)

        val result = testRestTemplate.postForEntity(uri, null, OwnershipDto::class.java).body!!
        val savedShortOwnership = enrichmentOwnershipService.get(shortOwnership.id)!!

        assertThat(savedShortOwnership.bestSellOrder).isEqualTo(shortBestSell)

        assertThat(result.bestSellOrder!!.id).isEqualTo(unionBestSell.id)

        coVerify {
            testOwnershipEventProducer.send(match<KafkaMessage<OwnershipEventDto>> { message ->
                message.value.ownershipId == ethOwnershipId
            })
        }
    }

    @Test
    fun `reconcile item`() = runBlocking<Unit> {
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

        val uri = "$baseUri/v0.1/refresh/item/${ethItemId.fullId()}/reconcile"

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

        val result = testRestTemplate.postForEntity(uri, null, ItemDto::class.java).body!!
        val savedShortItem = enrichmentItemService.get(shortItem.id)!!

        assertThat(savedShortItem.bestSellOrder!!.id).isEqualTo(shortBestSell.id)
        assertThat(savedShortItem.bestBidOrder!!.id).isEqualTo(shortBestBid.id)
        assertThat(savedShortItem.bestSellOrders[unionBestSell.sellCurrencyId]!!.id).isEqualTo(shortBestSell.id)
        assertThat(savedShortItem.bestBidOrders[unionBestBid.bidCurrencyId]!!.id).isEqualTo(shortBestBid.id)

        assertThat(result.bestSellOrder!!.id).isEqualTo(unionBestSell.id)
        assertThat(result.bestBidOrder!!.id).isEqualTo(unionBestBid.id)

        coVerify {
            testItemEventProducer.send(match<KafkaMessage<ItemEventDto>> { message ->
                message.value.itemId == ethItemId
            })
        }
    }

    @Test
    fun `reconcile ownership`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val ethOwnershipId = randomEthOwnershipId(ethItemId)
        val ethOwnership = randomEthOwnershipDto(ethOwnershipId)
        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, ethOwnershipId.blockchain)
        val shortOwnership = ShortOwnershipConverter.convert(unionOwnership)

        val ethBestSell = randomEthLegacySellOrderDto(ethItemId)
        val unionBestSell = ethOrderConverter.convert(ethBestSell, ethOwnershipId.blockchain)
        val shortBestSell = ShortOrderConverter.convert(unionBestSell)

        val uri = "$baseUri/v0.1/refresh/ownership/${ethOwnershipId.fullId()}/reconcile"

        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(ethOwnershipId, ethOwnership)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(ethItemId, ethBestSell.take.assetType)
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(
            ethOwnershipId,
            unionBestSell.sellCurrencyId,
            ethBestSell
        )

        val result = testRestTemplate.postForEntity(uri, null, OwnershipDto::class.java).body!!
        val savedShortOwnership = enrichmentOwnershipService.get(shortOwnership.id)!!

        assertThat(savedShortOwnership.bestSellOrder!!.id).isEqualTo(shortBestSell.id)
        assertThat(savedShortOwnership.bestSellOrders[unionBestSell.sellCurrencyId]!!.id).isEqualTo(shortBestSell.id)

        assertThat(result.bestSellOrder!!.id).isEqualTo(unionBestSell.id)

        coVerify {
            testOwnershipEventProducer.send(match<KafkaMessage<OwnershipEventDto>> { message ->
                message.value.ownershipId == ethOwnershipId
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

        val uri = "$baseUri/v0.1/refresh/item/${ethItemId.fullId()}/reconcile"

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

        val result = testRestTemplate.postForEntity(uri, null, ItemDto::class.java).body!!
        assertThat(result.deleted).isTrue

        coVerify {
            testItemEventProducer.send(match<KafkaMessage<ItemEventDto>> { message ->
                message.value is ItemDeleteEventDto && message.value.itemId == ethItemId
            })
        }
    }
}
