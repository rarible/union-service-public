package com.rarible.protocol.union.listener.service

import com.rarible.protocol.union.core.model.UnionCollectionChangeEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.core.model.stubEventMark
import com.rarible.protocol.union.enrichment.converter.CollectionDtoConverter
import com.rarible.protocol.union.enrichment.converter.EnrichmentCollectionConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentCollection
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthBidOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionAsset
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address

@IntegrationTest
class EnrichmentCollectionEventServiceIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var collectionService: EnrichmentCollectionService

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @Autowired
    private lateinit var collectionEventService: EnrichmentCollectionEventService

    @Test
    fun `on collection changed - ok`() = runWithKafka {
        val collectionId = randomEthCollectionId()
        val ethCollection = randomEthCollectionDto().copy(id = Address.apply(collectionId.value))
        val unionCollection = EthCollectionConverter.convert(ethCollection, collectionId.blockchain)
        val enrichmentCollection = EnrichmentCollectionConverter.convert(unionCollection)
        collectionService.save(enrichmentCollection)!!

        coEvery { testEthereumCollectionApi.getNftCollectionById(collectionId.value) } returns ethCollection.toMono()

        collectionEventService.onCollectionChanged(UnionCollectionChangeEvent(collectionId, stubEventMark()))

        val expected = CollectionDtoConverter.convertLegacy(unionCollection)

        waitAssert {
            val messages = findCollectionUpdates(collectionId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.collectionId).isEqualTo(collectionId)
            // TODO COLLECTION update meta check after the migration
            assertThat(messages[0].value.collection.copy(meta = null)).isEqualTo(expected)
        }
    }

    @Test
    fun `on collection updated - ok, collection inserted`() = runWithKafka {
        val collectionId = randomEthCollectionId()
        val ethCollection = randomEthCollectionDto().copy(id = Address.apply(collectionId.value))

        val unionCollection = EthCollectionConverter.convert(ethCollection, collectionId.blockchain)
        collectionEventService.onCollectionUpdate(UnionCollectionUpdateEvent(unionCollection, stubEventMark()))

        val updated = collectionService.get(EnrichmentCollectionId(collectionId))!!
        val expected = CollectionDtoConverter.convertLegacy(unionCollection)

        assertThat(updated.version).isEqualTo(0L)
        waitAssert {
            val messages = findCollectionUpdates(collectionId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.collectionId).isEqualTo(collectionId)
            // TODO COLLECTION update meta check after the migration
            assertThat(messages[0].value.collection.copy(meta = null)).isEqualTo(expected)
        }
    }

    @Test
    fun `on collection updated - ok, collection exists`() = runWithKafka {
        val collectionId = randomEthCollectionId()
        val enrichmentCollection = randomEnrichmentCollection(collectionId)
        val ethCollection = randomEthCollectionDto().copy(id = Address.apply(collectionId.value))

        val current = collectionService.save(enrichmentCollection)!!

        val unionCollection = EthCollectionConverter.convert(ethCollection, collectionId.blockchain)
        collectionEventService.onCollectionUpdate(UnionCollectionUpdateEvent(unionCollection, stubEventMark()))

        val updated = collectionService.get(current.id)!!
        val expected = CollectionDtoConverter.convertLegacy(unionCollection)

        assertThat(updated.version).isGreaterThan(current.version)
        waitAssert {
            val messages = findCollectionUpdates(collectionId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.collectionId).isEqualTo(collectionId)
            // TODO COLLECTION update meta check after the migration
            assertThat(messages[0].value.collection.copy(meta = null)).isEqualTo(expected)
        }
    }

    @Test
    fun `on best sell order updated - collection exists`() = runWithKafka {
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
        val expected = CollectionDtoConverter.convertLegacy(unionCollection, enrichmentCollection)
            .copy(bestSellOrder = unionBestSell)

        val saved = collectionService.get(enrichmentCollection.id)!!
        assertThat(saved.bestSellOrder).isEqualTo(ShortOrderConverter.convert(unionBestSell))

        waitAssert {
            val messages = findCollectionUpdates(collectionId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.collectionId).isEqualTo(collectionId)
            assertThat(messages[0].value.collection.id).isEqualTo(expected.id)
            assertThat(messages[0].value.collection.status).isEqualTo(expected.status)
            assertThat(messages[0].value.collection.bestSellOrder!!.id).isEqualTo(expected.bestSellOrder!!.id)
            assertThat(messages[0].value.collection.bestBidOrder).isNull()
        }
    }

    @Test
    fun `on best bid order updated - collection exists`() = runWithKafka {
        val collectionId = randomEthCollectionId()
        val enrichmentCollection = randomEnrichmentCollection(collectionId)
        val ethItem = randomEthCollectionDto().copy(id = Address.apply(collectionId.value))
        val unionCollection = EthCollectionConverter.convert(ethItem, collectionId.blockchain)
        collectionService.save(enrichmentCollection)

        val bestBidOrder = randomEthBidOrderDto().copy(
            take = randomEthCollectionAsset(Address.apply(collectionId.value))
        )
        val unionBestBid = ethOrderConverter.convert(bestBidOrder, collectionId.blockchain)

        coEvery { testEthereumCollectionApi.getNftCollectionById(collectionId.value) } returns ethItem.toMono()

        collectionEventService.onCollectionBestBidOrderUpdate(collectionId, unionBestBid, stubEventMark(), true)

        // In result event for Item we expect updated bestSellOrder
        val expected = CollectionDtoConverter.convertLegacy(unionCollection).copy(bestBidOrder = unionBestBid)

        val saved = collectionService.get(enrichmentCollection.id)!!
        assertThat(saved.bestBidOrder).isEqualTo(ShortOrderConverter.convert(unionBestBid))

        waitAssert {
            val messages = findCollectionUpdates(collectionId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.collectionId).isEqualTo(collectionId)
            assertThat(messages[0].value.collection.id).isEqualTo(expected.id)
            assertThat(messages[0].value.collection.bestBidOrder!!.id).isEqualTo(expected.bestBidOrder!!.id)
            assertThat(messages[0].value.collection.bestSellOrder).isNull()
        }
    }
}
