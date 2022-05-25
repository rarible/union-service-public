package com.rarible.protocol.union.listener.service

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc20
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionAsset
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthV2OrderDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.clearMocks
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import java.math.BigInteger

@IntegrationTest
class EnrichmentRefreshServiceTest : AbstractIntegrationTest(){

    @Autowired
    private lateinit var enrichmentRefreshService: EnrichmentRefreshService

    @BeforeEach
    fun beforeEach() {
        clearMocks(testEthereumOrderApi)
        clearMocks(testEthereumCollectionApi)
    }

    @Test
    fun `reconcile collection null best sell bid`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        val fakeItemId = ItemIdDto(BlockchainDto.ETHEREUM, collectionId.value, BigInteger("-1"))
        val currencyAsset = randomEthAssetErc20()
        val currencyId = EthConverter.convert(currencyAsset, BlockchainDto.ETHEREUM).type.ext.currencyAddress()

        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(fakeItemId, currencyAsset.assetType)
        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(fakeItemId, currencyAsset.assetType)

        val sellOrder = randomEthV2OrderDto()
        val bidOrder = randomEthV2OrderDto()

        val ethCollectionDto = randomEthCollectionDto(Address.apply(collectionId.value))

        coEvery { testEthereumCollectionApi.getNftCollectionById(collectionId.value) } returns ethCollectionDto.toMono()

        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(fakeItemId, currencyId, sellOrder)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(fakeItemId, currencyId, bidOrder)

        val updateEvent = enrichmentRefreshService.reconcileCollection(collectionId)

        assertThat(updateEvent.collectionId).isEqualTo(collectionId)
        val collectionUpdateEvent = updateEvent as CollectionUpdateEventDto

        assertThat(collectionUpdateEvent.collection.bestBidOrder).isNull()
        assertThat(collectionUpdateEvent.collection.bestSellOrder).isNull()
    }

    @Test
    fun `reconcile collection`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        val currencyAsset = randomEthAssetErc20()
        val currencyId = EthConverter.convert(currencyAsset, BlockchainDto.ETHEREUM).type.ext.currencyAddress()

        val fakeItemId = ItemIdDto(BlockchainDto.ETHEREUM, collectionId.value, BigInteger("-1"))

        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(fakeItemId, currencyAsset.assetType)
        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(fakeItemId, currencyAsset.assetType)

        val sellOrder = randomEthV2OrderDto().copy(
            make = randomEthCollectionAsset(Address.apply(collectionId.value))
        )
        val bidOrder = randomEthV2OrderDto().copy(
            take = randomEthCollectionAsset(Address.apply(collectionId.value)),
            make = currencyAsset
        )

        val ethCollectionDto = randomEthCollectionDto(Address.apply(collectionId.value))

        coEvery { testEthereumCollectionApi.getNftCollectionById(collectionId.value) } returns ethCollectionDto.toMono()

        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(fakeItemId, currencyId, sellOrder)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(fakeItemId, currencyId, bidOrder)

        val updateEvent = enrichmentRefreshService.reconcileCollection(collectionId)

        assertThat(updateEvent.collectionId).isEqualTo(collectionId)
        val collectionUpdateEvent = updateEvent as CollectionUpdateEventDto

        assertThat(collectionUpdateEvent.collection.bestBidOrder!!.take.type.ext.isCollection).isTrue
        assertThat(collectionUpdateEvent.collection.bestBidOrder!!.take.type.ext.collectionId).isEqualTo(collectionId)
        assertThat(collectionUpdateEvent.collection.bestSellOrder!!.make.type.ext.isCollection).isTrue
        assertThat(collectionUpdateEvent.collection.bestSellOrder!!.make.type.ext.collectionId).isEqualTo(collectionId)
    }
}