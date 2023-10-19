package com.rarible.protocol.union.listener.service

import com.rarible.protocol.union.core.model.UnionCollectionChangeEvent
import com.rarible.protocol.union.core.model.UnionCollectionSetBaseUriEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.core.model.stubEventMark
import com.rarible.protocol.union.enrichment.converter.CollectionDtoConverter
import com.rarible.protocol.union.enrichment.converter.EnrichmentCollectionConverter
import com.rarible.protocol.union.enrichment.converter.OrderDtoConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.repository.MetaAutoRefreshStateRepository
import com.rarible.protocol.union.enrichment.repository.MetaRefreshRequestRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc20
import com.rarible.protocol.union.integration.ethereum.data.randomEthBidOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionAsset
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

@IntegrationTest
class EnrichmentCollectionEventServiceIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var collectionService: EnrichmentCollectionService

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @Autowired
    private lateinit var collectionEventService: EnrichmentCollectionEventService

    @Autowired
    private lateinit var metaRefreshRequestRepository: MetaRefreshRequestRepository

    @Autowired
    private lateinit var metaAutoRefreshStateRepository: MetaAutoRefreshStateRepository

    @Test
    fun `on collection changed - ok`() = runBlocking {
        val collectionId = randomEthCollectionId()
        val ethCollection = randomEthCollectionDto().copy(id = Address.apply(collectionId.value))
        val unionCollection = EthCollectionConverter.convert(ethCollection, collectionId.blockchain)
        val enrichmentCollection = EnrichmentCollectionConverter.convert(unionCollection)
        collectionService.save(enrichmentCollection)!!

        collectionEventService.onCollectionChanged(UnionCollectionChangeEvent(collectionId, stubEventMark()))

        val expected = CollectionDtoConverter.convert(enrichmentCollection)

        waitAssert {
            val messages = findCollectionUpdates(collectionId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].collectionId).isEqualTo(collectionId)
            // TODO COLLECTION update meta check after the migration
            assertThat(messages[0].collection.copy(meta = null)).isEqualTo(expected)
        }
    }

    @Test
    fun `on collection updated - ok, collection inserted`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        val ethCollection = randomEthCollectionDto().copy(id = Address.apply(collectionId.value), scam = true)

        val unionCollection = EthCollectionConverter.convert(ethCollection, collectionId.blockchain)
        collectionEventService.onCollectionUpdate(UnionCollectionUpdateEvent(unionCollection, stubEventMark()))

        val updated = collectionService.get(EnrichmentCollectionId(collectionId))!!
        val expected = CollectionDtoConverter.convert(updated)

        waitAssert {
            val messages = findCollectionUpdates(collectionId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].collectionId).isEqualTo(collectionId)
            assertThat(messages[0].collection.scam).isTrue
            // TODO COLLECTION update meta check after the migration
            assertThat(messages[0].collection.copy(meta = null)).isEqualTo(expected)
        }
        val refreshState = metaAutoRefreshStateRepository.loadToCheckCreated(Instant.now().minusSeconds(60)).toList()
        assertThat(refreshState.map { it.id }).containsExactly(collectionId.fullId())

        collectionEventService.onCollectionUpdate(
            UnionCollectionUpdateEvent(
                unionCollection.copy(scam = false),
                stubEventMark()
            )
        )

        waitAssert {
            val messages = findCollectionUpdates(collectionId.value)
            assertThat(messages).hasSize(2)
            assertThat(messages[1].collectionId).isEqualTo(collectionId)
            assertThat(messages[1].collection.scam).isFalse
        }
    }

    @Test
    fun `on collection updated - ok, collection exists`() = runBlocking {
        val collectionId = randomEthCollectionId()
        val enrichmentCollection = randomEnrichmentCollection(collectionId)
        val ethCollection = randomEthCollectionDto().copy(id = Address.apply(collectionId.value))

        val current = collectionService.save(enrichmentCollection)!!

        val unionCollection = EthCollectionConverter.convert(ethCollection, collectionId.blockchain)
        collectionEventService.onCollectionUpdate(UnionCollectionUpdateEvent(unionCollection, stubEventMark()))

        val updated = collectionService.get(current.id)!!
        val expected = CollectionDtoConverter.convert(updated)

        assertThat(updated.version).isGreaterThan(current.version)
        waitAssert {
            val messages = findCollectionUpdates(collectionId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].collectionId).isEqualTo(collectionId)
            // TODO COLLECTION update meta check after the migration
            assertThat(messages[0].collection.copy(meta = null)).isEqualTo(expected)
        }
        val refreshState = metaAutoRefreshStateRepository.loadToCheckCreated(Instant.now().minusSeconds(60)).toList()
        assertThat(refreshState).isEmpty()
    }

    @Test
    fun `on best sell order updated - collection exists`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        val enrichmentCollection = randomEnrichmentCollection(collectionId)
        val ethCollection = randomEthCollectionDto().copy(id = Address.apply(collectionId.value))
        val unionCollection = EthCollectionConverter.convert(ethCollection, collectionId.blockchain)
        collectionService.save(enrichmentCollection)

        val bestSellOrder = randomEthSellOrderDto().copy(
            make = randomEthCollectionAsset(Address.apply(collectionId.value))
        )
        val unionBestSell = ethOrderConverter.convert(bestSellOrder, collectionId.blockchain)

        coEvery { testEthereumCollectionApi.getNftCollectionById(collectionId.value) } returns ethCollection.toMono()

        collectionEventService.onCollectionBestSellOrderUpdate(collectionId, unionBestSell, null, true)

        // In result event for Item we expect updated bestSellOrder
        val expected = CollectionDtoConverter.convert(enrichmentCollection)
            .copy(bestSellOrder = OrderDtoConverter.convert(unionBestSell))

        val saved = collectionService.get(enrichmentCollection.id)!!
        assertThat(saved.bestSellOrder).isEqualTo(ShortOrderConverter.convert(unionBestSell))

        waitAssert {
            val messages = findCollectionUpdates(collectionId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].collectionId).isEqualTo(collectionId)
            assertThat(messages[0].collection.id).isEqualTo(expected.id)
            assertThat(messages[0].collection.status).isEqualTo(expected.status)
            assertThat(messages[0].collection.bestSellOrder!!.id).isEqualTo(expected.bestSellOrder!!.id)
            assertThat(messages[0].collection.bestBidOrder).isNull()
        }
    }

    @Test
    fun `on best bid order updated - collection exists`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        val enrichmentCollection = randomEnrichmentCollection(collectionId)
        val ethItem = randomEthCollectionDto().copy(id = Address.apply(collectionId.value))

        val bestBidOrder = randomEthBidOrderDto().copy(
            take = randomEthCollectionAsset(Address.apply(collectionId.value)),
        )
        val unionBestBid = ethOrderConverter.convert(bestBidOrder, collectionId.blockchain)
        val whitelistedBidOrder = randomEthBidOrderDto().copy(
            take = randomEthCollectionAsset(Address.apply(collectionId.value)),
            make = randomEthAssetErc20(Address.apply("0xc778417e063141139fce010982780140aa0cd5ab")).copy(
                value = BigInteger.ZERO,
                valueDecimal = BigDecimal.ZERO,
            ),
        )
        val notWhitelistedBidOrder = randomEthBidOrderDto().copy(
            take = randomEthCollectionAsset(Address.apply(collectionId.value)),
            make = randomEthAssetErc20().copy(
                value = BigInteger.ZERO,
                valueDecimal = BigDecimal.ZERO,
            ),
        )
        val unionWhitelistedBidOrder = ethOrderConverter.convert(whitelistedBidOrder, collectionId.blockchain)
        val unionNotWhitelistedBidOrder = ethOrderConverter.convert(notWhitelistedBidOrder, collectionId.blockchain)

        collectionService.save(enrichmentCollection.copy(
            bestBidOrders = listOf(
                unionWhitelistedBidOrder,
                unionNotWhitelistedBidOrder
            ).associate { it.bidCurrencyId() to ShortOrderConverter.convert(it) }
        ))

        ethereumOrderControllerApiMock.mockGetByIds(whitelistedBidOrder)

        collectionEventService.onCollectionBestBidOrderUpdate(collectionId, unionBestBid, stubEventMark(), true)

        // In result event for Item we expect updated bestSellOrder
        val expected = CollectionDtoConverter.convert(enrichmentCollection)
            .copy(
                bestBidOrder = OrderDtoConverter.convert(unionBestBid),
                bestBidOrdersByCurrency = listOf(OrderDtoConverter.convert(unionWhitelistedBidOrder)),
            )

        val saved = collectionService.get(enrichmentCollection.id)!!
        assertThat(saved.bestBidOrder).isEqualTo(ShortOrderConverter.convert(unionBestBid))

        waitAssert {
            val messages = findCollectionUpdates(collectionId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].collectionId).isEqualTo(collectionId)
            assertThat(messages[0].collection.id).isEqualTo(expected.id)
            assertThat(messages[0].collection.bestBidOrder!!.id).isEqualTo(expected.bestBidOrder!!.id)
            assertThat(messages[0].collection.bestSellOrder).isNull()
            assertThat(messages[0].collection.bestBidOrdersByCurrency!!.map { it.id })
                .isEqualTo(expected.bestBidOrdersByCurrency!!.map { it.id })
        }
    }

    @Test
    fun `on collection setBaseUri - ok, refresh scheduled`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()

        collectionEventService.onCollectionSetBaseUri(
            UnionCollectionSetBaseUriEvent(
                collectionId = collectionId,
                eventTimeMarks = stubEventMark()
            )
        )

        val refreshRequest = metaRefreshRequestRepository.findToScheduleAndUpdate(1).first()

        assertThat(refreshRequest.collectionId).isEqualTo(collectionId.fullId())
        assertThat(refreshRequest.full).isTrue
    }
}
