package com.rarible.protocol.union.api.controller.internal

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.NftActivityFilterByItemDto
import com.rarible.protocol.dto.OrderActivityFilterByItemDto
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.core.model.download.MetaSource
import com.rarible.protocol.union.core.producer.UnionInternalCollectionEventProducer
import com.rarible.protocol.union.core.producer.UnionInternalItemEventProducer
import com.rarible.protocol.union.core.producer.UnionInternalOwnershipEventProducer
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipSourceDto
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashConverter
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashNftMetadataUpdate
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.repository.RawMetaCacheRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc1155
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc20
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthBidOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionAsset
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMintActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderActivityMatch
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthV2OrderDto
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import java.math.BigInteger

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

    @Autowired
    lateinit var rawMetaCacheRepository: RawMetaCacheRepository

    @MockkBean
    lateinit var internalItemProducer: UnionInternalItemEventProducer

    @MockkBean
    lateinit var internalCollectionProducer: UnionInternalCollectionEventProducer

    @MockkBean
    lateinit var internalOwnershipProducer: UnionInternalOwnershipEventProducer

    private val origin = "0xWhitelabel"
    private val ethOriginCollection = "0xf3348949db80297c78ec17d19611c263fc61f988" // from application.yaml
    private val active = listOf(com.rarible.protocol.dto.OrderStatusDto.ACTIVE)

    @BeforeEach
    fun setUp() {
        coEvery { internalItemProducer.sendDeleteEvent(any()) } returns Unit
        coEvery { internalItemProducer.sendChangeEvent(any()) } returns Unit
        coEvery { internalCollectionProducer.sendChangeEvent(any()) } returns Unit
        coEvery { internalOwnershipProducer.sendChangeEvent(any()) } returns Unit
    }

    @Test
    fun `reconcile item - full`() = runBlocking<Unit> {
        val itemId = ItemIdDto(BlockchainDto.ETHEREUM, ethOriginCollection, randomBigInt())
        val ethItem = randomEthNftItemDto(itemId)
        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)
        val shortItem = ShortItemConverter.convert(unionItem)

        val ethBestSell = randomEthSellOrderDto(itemId)
        val unionBestSell = ethOrderConverter.convert(ethBestSell, itemId.blockchain)
        val shortBestSell = ShortOrderConverter.convert(unionBestSell)

        val ethBestBid = randomEthBidOrderDto(itemId)
        val unionBestBid = ethOrderConverter.convert(ethBestBid, itemId.blockchain)
        val shortBestBid = ShortOrderConverter.convert(unionBestBid)

        val bidCurrency = unionBestBid.bidCurrencyId()
        val sellCurrency = unionBestSell.sellCurrencyId()

        // Amm order should be best sell, but it should not be an origin best sell order
        val ethAmmOrder = ethBestSell.copy(
            hash = Word.apply(randomWord()),
            make = ethBestSell.make.copy(value = ethBestSell.make.value.minus(BigInteger.TEN))
        )

        val unionAmmOrder = ethOrderConverter.convert(ethAmmOrder, itemId.blockchain)
        val shortAmmOrder = ShortOrderConverter.convert(unionAmmOrder)

        val ethOriginBestSell = randomEthSellOrderDto(itemId).copy(take = ethBestSell.take)
        val unionOriginBestSell = ethOrderConverter.convert(ethOriginBestSell, itemId.blockchain)
        val shortOriginBestSell = ShortOrderConverter.convert(unionOriginBestSell)

        val ethOriginBestBid = randomEthBidOrderDto(itemId).copy(make = ethBestBid.make)
        val unionOriginBestBid = ethOrderConverter.convert(ethOriginBestBid, itemId.blockchain)
        val shortOriginBestBid = ShortOrderConverter.convert(unionOriginBestBid)

        val ethAuction = randomEthAuctionDto(itemId)
        val auction = ethAuctionConverter.convert(ethAuction, BlockchainDto.ETHEREUM)

        // Fully auctioned ownership, should not be saved, but disguised event is expected for it
        val ethAuctionedOwnershipId = itemId.toOwnership(auction.seller.value)
        val auctionOwnershipId = itemId.toOwnership(auction.contract.value)
        val auctionOwnership = randomEthOwnershipDto(auctionOwnershipId)

        // Free ownership - should be reconciled in regular way
        val ethFreeOwnershipId = itemId.toOwnership(ethAmmOrder.maker.prefixed())
        val ethOwnership = randomEthOwnershipDto(ethFreeOwnershipId)
        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, ethFreeOwnershipId.blockchain)
        val shortOwnership = ShortOwnershipConverter.convert(unionOwnership)

        // Last sell activity for item
        val swapDto = randomEthOrderActivityMatch()
        val activity = swapDto.copy(left = swapDto.left.copy(asset = randomEthAssetErc1155(itemId)))

        ethereumOwnershipControllerApiMock.mockGetNftOwnershipsByItem(itemId, null, 1000, ethOwnership)
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(auctionOwnershipId, auctionOwnership)
        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(itemId, listOf(ethAuction))
        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)
        ethereumOrderControllerApiMock.mockGetAmmOrdersByItem(itemId, ethAmmOrder)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(itemId, active, ethBestSell.take.assetType)
        // Best sell for Item
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(itemId, sellCurrency, ethBestSell)
        // Same best sell for free Ownership
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(ethFreeOwnershipId, sellCurrency, ethBestSell)
        // Best sell for Item's origin
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(
            itemId, sellCurrency, origin, ethOriginBestSell
        )
        // Same best sell for Ownership origin
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(
            ethFreeOwnershipId, sellCurrency, origin, ethOriginBestSell
        )

        val mintActivity = randomEthItemMintActivity().copy(owner = Address.apply(ethFreeOwnershipId.owner.value))
        ethereumActivityControllerApiMock.mockGetNftActivitiesByItem(
            itemId,
            listOf(NftActivityFilterByItemDto.Types.MINT),
            1,
            null,
            ActivitySortDto.LATEST_FIRST,
            mintActivity
        )

        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(itemId, active, ethBestBid.make.assetType)
        // Item best bid
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(itemId, bidCurrency, ethBestBid)
        // Item's origin best bid
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(itemId, bidCurrency, origin, ethOriginBestBid)

        mockLastSellActivity(itemId, activity)

        val uri = "$baseUri/v0.1/refresh/item/${itemId.fullId()}/reconcile?full=true"
        val reconciled = testRestTemplate.postForEntity(uri, null, ItemDto::class.java).body!!
        val savedShortItem = enrichmentItemService.get(shortItem.id)!!
        val savedShortOwnership = enrichmentOwnershipService.get(shortOwnership.id)!!

        val itemOriginOrders = savedShortItem.originOrders.toList()[0]
        val ownershipOriginOrders = savedShortOwnership.originOrders.toList()[0]

        assertThat(savedShortItem.bestSellOrder!!.id).isEqualTo(shortAmmOrder.id)
        assertThat(savedShortItem.bestBidOrder!!.id).isEqualTo(shortBestBid.id)
        assertThat(savedShortItem.bestSellOrders[unionBestSell.sellCurrencyId()]!!.id).isEqualTo(shortAmmOrder.id)
        assertThat(savedShortItem.bestBidOrders[unionBestBid.bidCurrencyId()]!!.id).isEqualTo(shortBestBid.id)
        assertThat(savedShortItem.auctions).isEqualTo(setOf(auction.id))
        assertThat(savedShortItem.lastSale!!.date).isEqualTo(activity.date)
        assertThat(savedShortItem.poolSellOrders).hasSize(1)
        assertThat(savedShortItem.poolSellOrders[0].order).isEqualTo(shortAmmOrder)

        assertThat(itemOriginOrders.bestSellOrder!!.id).isEqualTo(shortOriginBestSell.id)
        assertThat(itemOriginOrders.bestBidOrder!!.id).isEqualTo(shortOriginBestBid.id)

        assertThat(savedShortOwnership.source).isEqualTo(OwnershipSourceDto.MINT)
        assertThat(savedShortOwnership.bestSellOrder!!.id).isEqualTo(shortAmmOrder.id)
        assertThat(savedShortOwnership.bestSellOrders[unionBestSell.sellCurrencyId()]!!.id).isEqualTo(shortAmmOrder.id)

        assertThat(ownershipOriginOrders.bestSellOrder!!.id).isEqualTo(shortOriginBestSell.id)

        assertThat(reconciled.bestSellOrder!!.id).isEqualTo(unionAmmOrder.id)
        assertThat(reconciled.bestBidOrder!!.id).isEqualTo(unionBestBid.id)
        assertThat(reconciled.originOrders).hasSize(1)

        coVerify {
            internalItemProducer.sendChangeEvent(itemId)
        }
        coVerify(exactly = 1) {
            internalOwnershipProducer.sendChangeEvent(ethFreeOwnershipId)
        }
        coVerify(exactly = 1) {
            internalOwnershipProducer.sendChangeEvent(ethAuctionedOwnershipId)
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

        val ethBestSell = randomEthSellOrderDto(ethItemId)
        val unionBestSell = ethOrderConverter.convert(ethBestSell, ethOwnershipId.blockchain)
        val shortBestSell = ShortOrderConverter.convert(unionBestSell)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(
            ethItemId,
            ethOwnershipId.owner.value,
            listOf(ethAuction)
        )
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(ethOwnershipId, ethOwnership)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(
            ethItemId,
            active,
            ethBestSell.take.assetType
        )
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(
            ethOwnershipId,
            unionBestSell.sellCurrencyId(),
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

        ethereumOrderControllerApiMock.mockGetAmmOrdersByItem(ethItemId)

        val uri = "$baseUri/v0.1/refresh/ownership/${ethOwnershipId.fullId()}/reconcile"
        val reconciled = testRestTemplate.postForEntity(uri, null, OwnershipDto::class.java).body!!
        val savedShortOwnership = enrichmentOwnershipService.get(shortOwnership.id)!!

        assertThat(savedShortOwnership.source).isEqualTo(OwnershipSourceDto.MINT)
        assertThat(savedShortOwnership.bestSellOrder!!.id).isEqualTo(shortBestSell.id)
        assertThat(savedShortOwnership.bestSellOrders[unionBestSell.sellCurrencyId()]!!.id).isEqualTo(shortBestSell.id)

        assertThat(reconciled.bestSellOrder!!.id).isEqualTo(unionBestSell.id)
        assertThat(reconciled.auction).isEqualTo(auction)

        coVerify(exactly = 1) {
            internalOwnershipProducer.sendChangeEvent(ethOwnershipId)
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
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(ethItemId, active)

        val uri = "$baseUri/v0.1/refresh/ownership/${ethOwnershipId.fullId()}/reconcile"
        val reconciled = testRestTemplate.postForEntity(uri, null, OwnershipDto::class.java).body!!

        // Nothing to save - there should not be enrichment data for fully-auctioned ownerships
        assertThat(enrichmentOwnershipService.get(ShortOwnershipId(auctionOwnershipId))).isNull()
        assertThat(enrichmentOwnershipService.get(ShortOwnershipId(ethOwnershipId))).isNull()

        assertThat(reconciled.auction).isEqualTo(auction)

        coVerify(exactly = 1) {
            internalOwnershipProducer.sendChangeEvent(ethOwnershipId)
        }
    }

    @Test
    fun `reconcile deleted item`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId).copy(deleted = true)

        val ethBestSell = randomEthSellOrderDto(ethItemId)
        val unionBestSell = ethOrderConverter.convert(ethBestSell, ethItemId.blockchain)

        val ethBestBid = randomEthBidOrderDto(ethItemId)
        val unionBestBid = ethOrderConverter.convert(ethBestBid, ethItemId.blockchain)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(ethItemId, emptyList())
        ethereumItemControllerApiMock.mockGetNftItemById(ethItemId, ethItem)
        ethereumOrderControllerApiMock.mockGetAmmOrdersByItem(ethItemId)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(
            ethItemId,
            active,
            ethBestSell.take.assetType
        )
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(
            ethItemId,
            unionBestSell.sellCurrencyId(),
            ethBestSell
        )

        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(ethItemId, active, ethBestBid.make.assetType)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(
            ethItemId,
            unionBestBid.bidCurrencyId(),
            ethBestBid
        )
        mockLastSellActivity(ethItemId, null)

        val uri = "$baseUri/v0.1/refresh/item/${ethItemId.fullId()}/reconcile"
        val reconciled = testRestTemplate.postForEntity(uri, null, ItemDto::class.java).body!!
        assertThat(reconciled.deleted).isTrue()

        coVerify {
            internalItemProducer.sendDeleteEvent(ethItemId)
        }
    }

    @Test
    fun `reconcile collection`() = runBlocking<Unit> {
        val collectionId = CollectionIdDto(BlockchainDto.ETHEREUM, ethOriginCollection)
        val currencyAsset = randomEthAssetErc20()
        val currencyId = EthConverter.convert(currencyAsset, BlockchainDto.ETHEREUM).type.currencyId()!!

        val fakeItemId = ItemIdDto(BlockchainDto.ETHEREUM, collectionId.value, BigInteger("-1"))

        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(fakeItemId, active, currencyAsset.assetType)
        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(fakeItemId, active, currencyAsset.assetType)

        val sellOrder = randomEthV2OrderDto().copy(
            make = randomEthCollectionAsset(Address.apply(collectionId.value))
        )
        val unionBestSell = ethOrderConverter.convert(sellOrder, collectionId.blockchain)

        val bidOrder = randomEthV2OrderDto().copy(
            take = randomEthCollectionAsset(Address.apply(collectionId.value)),
            make = currencyAsset
        )
        val unionBestBid = ethOrderConverter.convert(bidOrder, collectionId.blockchain)

        val ethCollectionDto = randomEthCollectionDto(Address.apply(collectionId.value))

        coEvery { testEthereumCollectionApi.getNftCollectionById(collectionId.value) } returns ethCollectionDto.toMono()

        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(fakeItemId, currencyId, sellOrder)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(fakeItemId, currencyId, bidOrder)
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(fakeItemId, currencyId, origin, sellOrder)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(fakeItemId, currencyId, origin, bidOrder)

        val uri = "$baseUri/v0.1/refresh/collection/${collectionId.fullId()}/reconcile"
        val reconciled = testRestTemplate.postForEntity(uri, null, CollectionDto::class.java).body!!
        val originOrders = reconciled.originOrders!!.toList()[0]

        assertThat(reconciled.bestBidOrder!!.take.type.ext.isCollectionAsset).isTrue
        assertThat(reconciled.bestBidOrder!!.take.type.ext.collectionId).isEqualTo(collectionId)
        assertThat(reconciled.bestSellOrder!!.make.type.ext.isCollectionAsset).isTrue
        assertThat(reconciled.bestSellOrder!!.make.type.ext.collectionId).isEqualTo(collectionId)

        assertThat(originOrders.bestSellOrder!!.id).isEqualTo(unionBestSell.id)
        assertThat(originOrders.bestBidOrder!!.id).isEqualTo(unionBestBid.id)

        coVerify {
            internalCollectionProducer.sendChangeEvent(collectionId)
        }
    }

    @Test
    fun `reconcile collection - without best orders`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        val fakeItemId = ItemIdDto(BlockchainDto.ETHEREUM, collectionId.value, BigInteger("-1"))
        val currencyAsset = randomEthAssetErc20()
        val currencyId = EthConverter.convert(currencyAsset, BlockchainDto.ETHEREUM).type.currencyId()!!

        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(fakeItemId, active, currencyAsset.assetType)
        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(fakeItemId, active, currencyAsset.assetType)

        val sellOrder = randomEthV2OrderDto()
        val bidOrder = randomEthV2OrderDto()

        val ethCollectionDto = randomEthCollectionDto(Address.apply(collectionId.value))

        coEvery { testEthereumCollectionApi.getNftCollectionById(collectionId.value) } returns ethCollectionDto.toMono()

        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(fakeItemId, currencyId, sellOrder)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(fakeItemId, currencyId, bidOrder)

        val uri = "$baseUri/v0.1/refresh/collection/${collectionId.fullId()}/reconcile"
        val reconciled = testRestTemplate.postForEntity(uri, null, CollectionDto::class.java).body!!

        assertThat(reconciled.id).isEqualTo(collectionId)
        assertThat(reconciled.bestBidOrder).isNull()
        assertThat(reconciled.bestSellOrder).isNull()

        coVerify {
            internalCollectionProducer.sendChangeEvent(collectionId)
        }
    }

    // TODO should be moved to EnrichmentOrderServiceTest
    @Test
    fun `should ignore best sell order with filled taker`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId)
        val unionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val shortItem = ShortItemConverter.convert(unionItem)

        val ethBestSell = randomEthSellOrderDto(ethItemId).copy(taker = Address.ONE())
        val unionBestSell = ethOrderConverter.convert(ethBestSell, ethItemId.blockchain)

        val ethBestBid = randomEthBidOrderDto(ethItemId)
        val unionBestBid = ethOrderConverter.convert(ethBestBid, ethItemId.blockchain)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(ethItemId, emptyList())
        ethereumItemControllerApiMock.mockGetNftItemById(ethItemId, ethItem)
        ethereumOrderControllerApiMock.mockGetAmmOrdersByItem(ethItemId)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(
            ethItemId,
            active,
            ethBestSell.take.assetType
        )
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(
            ethItemId,
            unionBestSell.sellCurrencyId(),
            ethBestSell
        )

        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(ethItemId, active, ethBestBid.make.assetType)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(
            ethItemId,
            unionBestBid.bidCurrencyId(),
            ethBestBid
        )
        mockLastSellActivity(ethItemId, null)

        val uri = "$baseUri/v0.1/refresh/item/${ethItemId.fullId()}/reconcile"
        val reconciled = testRestTemplate.postForEntity(uri, null, ItemDto::class.java).body!!
        assertThat(reconciled.bestSellOrder).isNull()
        assertThat(reconciled.bestBidOrder).isNotNull()

        val savedShortItem = enrichmentItemService.get(shortItem.id)!!
        assertThat(savedShortItem.bestSellOrder).isNull()
        assertThat(savedShortItem.bestSellOrders).isEmpty()

        coVerify {
            internalItemProducer.sendChangeEvent(ethItemId)
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

        val ethBestSell = randomEthSellOrderDto(ethItemId)
        val ethBestSellWithTaker = randomEthSellOrderDto(ethItemId).copy(taker = Address.ONE())
        val unionBestSell = ethOrderConverter.convert(ethBestSell, ethItemId.blockchain)
        val shortBestSell = ShortOrderConverter.convert(unionBestSell)

        val ethBestBid = randomEthBidOrderDto(ethItemId)
        val unionBestBid = ethOrderConverter.convert(ethBestBid, ethItemId.blockchain)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(ethItemId, emptyList())
        ethereumItemControllerApiMock.mockGetNftItemById(ethItemId, ethItem)
        ethereumOrderControllerApiMock.mockGetAmmOrdersByItem(ethItemId)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(
            ethItemId,
            active,
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

        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(ethItemId, active, ethBestBid.make.assetType)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(
            ethItemId,
            unionBestBid.bidCurrencyId(),
            ethBestBid
        )
        mockLastSellActivity(ethItemId, null)

        val uri = "$baseUri/v0.1/refresh/item/${ethItemId.fullId()}/reconcile"
        val reconciled = testRestTemplate.postForEntity(uri, null, ItemDto::class.java).body!!
        assertThat(reconciled.bestSellOrder!!.id).isEqualTo(unionBestSell.id)

        val savedShortItem = enrichmentItemService.get(shortItem.id)!!
        assertThat(savedShortItem.bestSellOrder!!.id).isEqualTo(shortBestSell.id)
        assertThat(savedShortItem.bestSellOrders[unionBestSell.sellCurrencyId()]!!.id).isEqualTo(shortBestSell.id)

        coVerify {
            internalItemProducer.sendChangeEvent(ethItemId)
        }
    }

    @Test
    fun `handle simpleHash nft meta update webhook - ok, chain`() = runBlocking<Unit> {
        `check simpleHash nft meta update webhook`("chain.nft_metadata_update")
    }

    @Test
    fun `handle simpleHash nft meta update webhook - ok, contract`() = runBlocking<Unit> {
        `check simpleHash nft meta update webhook`("contract.nft_metadata_update")
    }

    fun `check simpleHash nft meta update webhook`(eventType: String) = runBlocking<Unit> {
        val eventJsom = getResource("/json/simplehash/nft_metadata_update.json")
            .replace("#EVENT_TYPE", eventType)

        val eventDto = jacksonObjectMapper().readValue(eventJsom, SimpleHashNftMetadataUpdate::class.java)

        val expectedItemId = "ethereum.0x8943c7bac1914c9a7aba750bf2b6b09fd21037e0.5903"
        val expectedItemIdDtp = IdParser.parseItemId(expectedItemId.replace(".", ":").uppercase())

        val uri = "$baseUri/v0.1/refresh/items/simplehash/metaUpdateWebhook"

        val code = testRestTemplate.postForEntity(uri, eventDto, Unit::class.java).statusCode
        assertThat(code).isEqualTo(HttpStatus.NO_CONTENT)

        coVerify(exactly = 1) {
            testDownloadTaskProducer.send(withArg<List<KafkaMessage<DownloadTask>>> {
                assertThat(it).hasSize(1)
                assertThat(it[0].value).isInstanceOf(DownloadTask::class.java)
                val task = it[0].value
                assertThat(task.id).isEqualTo(expectedItemIdDtp.fullId())
            })
        }
        val cache = rawMetaCacheRepository.get(SimpleHashConverter.cacheId(expectedItemIdDtp))
        assertThat(cache).isNotNull
        assertThat(cache!!.source).isEqualTo(MetaSource.SIMPLE_HASH)
        assertThat(cache.entityId).isEqualTo(expectedItemIdDtp.fullId())
        assertThat(SimpleHashConverter.convertRawToSimpleHashItem(cache.data).nftId).isEqualTo(expectedItemId)
    }

    @Test
    fun `handle simpleHash nft meta update webhook - false`() = runBlocking<Unit> {
        val uri = "$baseUri/v0.1/refresh/items/simplehash/metaUpdateWebhook"
        val code = testRestTemplate.postForEntity(uri, "other", Unit::class.java).statusCode
        assertThat(code).isEqualTo(HttpStatus.NO_CONTENT)

        coVerify(exactly = 0) {
            testDownloadTaskProducer.send(any<List<KafkaMessage<DownloadTask>>>())
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
