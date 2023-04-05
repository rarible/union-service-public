package com.rarible.protocol.union.api.controller

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.FlowNftCollectionsDto
import com.rarible.protocol.dto.NftCollectionsDto
import com.rarible.protocol.union.api.client.CollectionControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.util.PageSize
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.enrichment.converter.EnrichmentCollectionConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc20
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionAsset
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthV2OrderDto
import com.rarible.protocol.union.integration.flow.converter.FlowCollectionConverter
import com.rarible.protocol.union.integration.flow.data.randomFlowAddress
import com.rarible.protocol.union.integration.flow.data.randomFlowCollectionDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosAddress
import com.rarible.protocol.union.integration.tezos.data.randomTezosCollectionDto
import com.rarible.tzkt.model.CollectionType
import com.rarible.tzkt.model.Page
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.web.client.RestTemplate
import reactor.kotlin.core.publisher.toMono

@FlowPreview
@IntegrationTest
class CollectionControllerFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val size = PageSize.COLLECTION.default

    @Autowired
    lateinit var collectionControllerClient: CollectionControllerApi

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @Autowired
    lateinit var enrichmentCollectionService: EnrichmentCollectionService

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var testTemplate: RestTemplate

    private fun baseUrl(): String {
        return "http://localhost:$port/v0.1"
    }

    @Test
    fun `get collection by id - ethereum, enriched`() = runBlocking<Unit> {
        val collectionId = randomAddress()
        val collectionIdFull = EthConverter.convert(collectionId, BlockchainDto.ETHEREUM)
        val ethCollectionDto = randomEthCollectionDto(collectionId).copy(isRaribleContract = true)
        val ethUnionCollection = EthCollectionConverter.convert(ethCollectionDto, BlockchainDto.ETHEREUM)
        val collectionAsset = randomEthCollectionAsset(collectionId)
        val ethOrder = randomEthV2OrderDto(collectionAsset, randomAddress(), randomEthAssetErc20())
        val ethUnionOrder = ethOrderConverter.convert(ethOrder, BlockchainDto.ETHEREUM)

        val shortOrder = ShortOrderConverter.convert(ethUnionOrder)
        val enrichmentCollection = EnrichmentCollectionConverter.convert(ethUnionCollection)
            .copy(bestSellOrder = shortOrder)
        enrichmentCollectionService.save(enrichmentCollection)

        ethereumOrderControllerApiMock.mockGetByIds(ethOrder)
        coEvery { testEthereumCollectionApi.getNftCollectionById(collectionIdFull.value) } returns ethCollectionDto.copy(isRaribleContract = true).toMono()

        val unionCollection = collectionControllerClient.getCollectionById(collectionIdFull.fullId()).awaitFirst()

        assertThat(unionCollection.id.value).isEqualTo(collectionIdFull.value)
        assertThat(unionCollection.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
        assertThat(unionCollection.self).isTrue
        assertThat(unionCollection.bestSellOrder!!.id).isEqualTo(ethUnionOrder.id)
    }

    @Test
    fun `get collection by id - flow`() = runBlocking<Unit> {
        val collectionId = randomString()
        val collectionIdFull = UnionAddressConverter.convert(BlockchainDto.FLOW, collectionId)
        val collection = randomFlowCollectionDto(collectionId)

        val unionCollection = FlowCollectionConverter.convert(collection, BlockchainDto.FLOW)
        enrichmentCollectionService.save(EnrichmentCollectionConverter.convert(unionCollection))

        coEvery { testFlowCollectionApi.getNftCollectionById(collectionId) } returns collection.toMono()

        val flowCollection = collectionControllerClient.getCollectionById(collectionIdFull.fullId()).awaitFirst()

        assertThat(flowCollection.id.value).isEqualTo(collectionIdFull.value)
        assertThat(flowCollection.id.blockchain).isEqualTo(BlockchainDto.FLOW)
    }

    @Test
    fun `get collections by owner - ethereum`() = runBlocking<Unit> {
        val ethOwnerId = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val collection = randomEthCollectionDto()
        val collectionId = EthConverter.convert(collection.id, BlockchainDto.ETHEREUM)

        coEvery {
            testEthereumCollectionApi.searchNftCollectionsByOwner(ethOwnerId.value, continuation, size)
        } returns NftCollectionsDto(1, null, listOf(collection)).toMono()

        val unionCollections = collectionControllerClient.getCollectionsByOwner(
            ethOwnerId.fullId(), null, continuation, size
        ).awaitFirst()

        val ethCollection = unionCollections.collections[0]
        assertThat(ethCollection.id.value).isEqualTo(collectionId.value)
    }

    @Test
    fun `get collections by owner - tezos`() = runBlocking<Unit> {
        val tezosOwnerId = randomTezosAddress()
        val collection = randomTezosCollectionDto()
        val collectionId = UnionAddressConverter.convert(BlockchainDto.TEZOS, collection.address!!)

        coEvery {
            tzktCollectionClient.collectionsByOwner(tezosOwnerId.value, any(), any(), any())
        } returns Page(listOf(collection), null)
        coEvery {
            tzktCollectionClient.collectionType(any())
        } returns CollectionType.MT

        val unionCollections = collectionControllerClient.getCollectionsByOwner(
            tezosOwnerId.fullId(), null, continuation, size
        ).awaitFirst()

        val tezosCollection = unionCollections.collections[0]
        assertThat(tezosCollection.id.value).isEqualTo(collectionId.value)
    }

    @Test
    fun `get collections by owner - flow`() = runBlocking<Unit> {
        val flowOwnerId = randomFlowAddress()
        val collection = randomFlowCollectionDto()

        coEvery {
            testFlowCollectionApi.searchNftCollectionsByOwner(flowOwnerId.value, continuation, size)
        } returns FlowNftCollectionsDto(null, listOf(collection)).toMono()

        val unionCollections = collectionControllerClient.getCollectionsByOwner(
            flowOwnerId.fullId(), null, continuation, size
        ).awaitFirst()

        val flowCollection = unionCollections.collections[0]
        assertThat(flowCollection.id.value).isEqualTo(collection.id)
    }

    @Test
    fun `get all collections`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW, BlockchainDto.TEZOS)
        val ethContinuation = randomEthAddress()
        val continuation = CombinedContinuation(
            mapOf(
                BlockchainDto.ETHEREUM.toString() to ethContinuation
            )
        )
        val size = 10

        val flowCollections = listOf(randomFlowCollectionDto(), randomFlowCollectionDto())
        val ethCollections = listOf(randomEthCollectionDto(), randomEthCollectionDto(), randomEthCollectionDto())
        val tezosCollections = listOf(randomTezosCollectionDto())

        coEvery {
            testFlowCollectionApi.searchNftAllCollections(null, size)
        } returns FlowNftCollectionsDto(null, flowCollections).toMono()

        coEvery {
            testEthereumCollectionApi.searchNftAllCollections(ethContinuation, size)
        } returns NftCollectionsDto(3, null, ethCollections).toMono()

        coEvery {
            tzktCollectionClient.collectionsAll(size, any(), any())
        } returns Page(tezosCollections, null)
        coEvery {
            tzktCollectionClient.collectionType(any())
        } returns CollectionType.MT

        val unionCollections = collectionControllerClient.getAllCollections(
            blockchains, continuation.toString(), size
        ).awaitFirst()

        assertThat(unionCollections.collections).hasSize(6)
        assertThat(unionCollections.continuation).isNull()
    }
}
