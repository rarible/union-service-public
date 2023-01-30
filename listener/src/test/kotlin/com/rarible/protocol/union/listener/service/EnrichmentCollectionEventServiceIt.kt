package com.rarible.protocol.union.listener.service

import com.rarible.protocol.union.core.model.stubEventMark
import com.rarible.protocol.union.enrichment.converter.EnrichedCollectionConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import com.rarible.protocol.union.enrichment.test.data.randomShortCollection
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
import org.assertj.core.api.Assertions
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
    fun `on best sell order updated - collection exists`() = runWithKafka {
        val collectionId = randomEthCollectionId()
        val shortCollection = randomShortCollection(collectionId)
        val ethItem = randomEthCollectionDto().copy(id = Address.apply(collectionId.value))
        val unionCollection = EthCollectionConverter.convert(ethItem, collectionId.blockchain)
        collectionService.save(shortCollection)

        val bestSellOrder = randomEthSellOrderDto().copy(
            make = randomEthCollectionAsset(Address.apply(collectionId.value))
        )
        val unionBestSell = ethOrderConverter.convert(bestSellOrder, collectionId.blockchain)

        coEvery { testEthereumCollectionApi.getNftCollectionById(collectionId.value) } returns ethItem.toMono()

        collectionEventService.onCollectionBestSellOrderUpdate(collectionId, unionBestSell, null, true)

        // In result event for Item we expect updated bestSellOrder
        val expected = EnrichedCollectionConverter.convert(unionCollection, shortCollection)
            .copy(bestSellOrder = unionBestSell)

        val saved = collectionService.get(shortCollection.id)!!
        Assertions.assertThat(saved.bestSellOrder).isEqualTo(ShortOrderConverter.convert(unionBestSell))

        waitAssert {
            val messages = findCollectionUpdates(collectionId.value)
            Assertions.assertThat(messages).hasSize(1)
            Assertions.assertThat(messages[0].value.collectionId).isEqualTo(collectionId)
            Assertions.assertThat(messages[0].value.collection.id).isEqualTo(expected.id)
            Assertions.assertThat(messages[0].value.collection.status).isEqualTo(expected.status)
            Assertions.assertThat(messages[0].value.collection.bestSellOrder!!.id).isEqualTo(expected.bestSellOrder!!.id)
            Assertions.assertThat(messages[0].value.collection.bestBidOrder).isNull()
            Assertions.assertThat(messages[0].value.collection.statistics?.itemCount).isEqualTo(expected.statistics?.itemCount)
            Assertions.assertThat(messages[0].value.collection.statistics?.itemCountTotal).isEqualTo(expected.statistics?.itemCountTotal)
        }
    }

    @Test
    fun `on best bid order updated - collection exists`() = runWithKafka {
        val collectionId = randomEthCollectionId()
        val shortCollection = randomShortCollection(collectionId)
        val ethItem = randomEthCollectionDto().copy(id = Address.apply(collectionId.value))
        val unionCollection = EthCollectionConverter.convert(ethItem, collectionId.blockchain)
        collectionService.save(shortCollection)

        val bestBidOrder = randomEthBidOrderDto().copy(
            take = randomEthCollectionAsset(Address.apply(collectionId.value))
        )
        val unionBestBid = ethOrderConverter.convert(bestBidOrder, collectionId.blockchain)

        coEvery { testEthereumCollectionApi.getNftCollectionById(collectionId.value) } returns ethItem.toMono()

        collectionEventService.onCollectionBestBidOrderUpdate(collectionId, unionBestBid, stubEventMark(), true)

        // In result event for Item we expect updated bestSellOrder
        val expected = EnrichedCollectionConverter.convert(unionCollection).copy(bestBidOrder = unionBestBid)

        val saved = collectionService.get(shortCollection.id)!!
        Assertions.assertThat(saved.bestBidOrder).isEqualTo(ShortOrderConverter.convert(unionBestBid))

        waitAssert {
            val messages = findCollectionUpdates(collectionId.value)
            Assertions.assertThat(messages).hasSize(1)
            Assertions.assertThat(messages[0].value.collectionId).isEqualTo(collectionId)
            Assertions.assertThat(messages[0].value.collection.id).isEqualTo(expected.id)
            Assertions.assertThat(messages[0].value.collection.bestBidOrder!!.id).isEqualTo(expected.bestBidOrder!!.id)
            Assertions.assertThat(messages[0].value.collection.bestSellOrder).isNull()
        }
    }
}