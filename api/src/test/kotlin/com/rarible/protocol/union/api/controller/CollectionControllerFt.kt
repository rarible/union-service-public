package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.FlowNftCollectionsDto
import com.rarible.protocol.dto.NftCollectionsDto
import com.rarible.protocol.union.api.client.CollectionControllerApi
import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.flow.converter.FlowContractConverter
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.test.data.*
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono

@FlowPreview
@IntegrationTest
class CollectionControllerFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val size = PageSize.COLLECTION.default

    @Autowired
    lateinit var collectionControllerClient: CollectionControllerApi

    @Test
    fun `get collection by id - ethereum`() = runBlocking<Unit> {
        val collectionId = randomAddress()
        val collectionIdFull = UnionAddressConverter.convert(collectionId, BlockchainDto.ETHEREUM)
        val collection = randomEthCollectionDto(collectionId)

        coEvery { testEthereumCollectionApi.getNftCollectionById(collectionIdFull.value) } returns collection.toMono()

        val unionCollection = collectionControllerClient.getCollectionById(collectionIdFull.fullId()).awaitFirst()
        val ethCollection = unionCollection as EthCollectionDto

        assertThat(ethCollection.id.value).isEqualTo(collectionIdFull.value)
        assertThat(ethCollection.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
    }

    @Test
    fun `get collection by id - polygon`() = runBlocking<Unit> {
        val collectionId = randomAddress()
        val collectionIdFull = UnionAddressConverter.convert(collectionId, BlockchainDto.POLYGON)
        val collection = randomEthCollectionDto(collectionId)

        coEvery { testPolygonCollectionApi.getNftCollectionById(collectionIdFull.value) } returns collection.toMono()

        val unionCollection = collectionControllerClient.getCollectionById(collectionIdFull.fullId()).awaitFirst()
        val ethCollection = unionCollection as EthCollectionDto

        assertThat(ethCollection.id.value).isEqualTo(collectionIdFull.value)
        assertThat(ethCollection.id.blockchain).isEqualTo(BlockchainDto.POLYGON)
    }

    @Test
    fun `get collection by id - flow`() = runBlocking<Unit> {
        val collectionId = randomString()
        val collectionIdFull = FlowContractConverter.convert(collectionId, BlockchainDto.FLOW)
        val collection = randomFlowCollectionDto(collectionId)

        coEvery { testFlowCollectionApi.getNftCollectionById(collectionId) } returns collection.toMono()

        val unionCollection = collectionControllerClient.getCollectionById(collectionIdFull.fullId()).awaitFirst()
        val flowCollection = unionCollection as FlowCollectionDto

        assertThat(flowCollection.id.value).isEqualTo(collectionIdFull.value)
        assertThat(flowCollection.id.blockchain).isEqualTo(BlockchainDto.FLOW)
    }

    @Test
    fun `get collections by owner - ethereum`() = runBlocking<Unit> {
        val ethOwnerId = randomEthAddress()
        val collection = randomEthCollectionDto()
        val collectionId = UnionAddressConverter.convert(collection.id, BlockchainDto.ETHEREUM)

        coEvery {
            testEthereumCollectionApi.searchNftCollectionsByOwner(ethOwnerId.value, continuation, size)
        } returns NftCollectionsDto(1, null, listOf(collection)).toMono()

        val unionCollections = collectionControllerClient.getCollectionsByOwner(
            ethOwnerId.fullId(), continuation, size
        ).awaitFirst()

        val ethCollection = unionCollections.collections[0] as EthCollectionDto
        assertThat(ethCollection.id.value).isEqualTo(collectionId.value)
    }

    @Test
    fun `get collections by owner - polygon`() = runBlocking<Unit> {
        val polyOwnerId = randomPolygonAddress()
        val collection = randomEthCollectionDto()
        val collectionId = UnionAddressConverter.convert(collection.id, BlockchainDto.POLYGON)

        coEvery {
            testPolygonCollectionApi.searchNftCollectionsByOwner(polyOwnerId.value, continuation, size)
        } returns NftCollectionsDto(1, null, listOf(collection)).toMono()

        val unionCollections = collectionControllerClient.getCollectionsByOwner(
            polyOwnerId.fullId(), continuation, size
        ).awaitFirst()

        val polyCollection = unionCollections.collections[0] as EthCollectionDto
        assertThat(polyCollection.id.value).isEqualTo(collectionId.value)
    }

    @Test
    fun `get collections by owner - flow`() = runBlocking<Unit> {
        val flowOwnerId = randomFlowAddress()
        val collection = randomFlowCollectionDto()

        coEvery {
            testFlowCollectionApi.searchNftCollectionsByOwner(flowOwnerId.value, continuation, size)
        } returns FlowNftCollectionsDto(1, null, listOf(collection)).toMono()

        val unionCollections = collectionControllerClient.getCollectionsByOwner(
            flowOwnerId.fullId(), continuation, size
        ).awaitFirst()

        val flowCollection = unionCollections.collections[0] as FlowCollectionDto
        assertThat(flowCollection.id.value).isEqualTo(collection.id)
    }

    @Test
    fun `get all collections`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)
        val continuation = "${nowMillis()}_${randomString()}"
        val size = 10

        val flowCollections = listOf(randomFlowCollectionDto(), randomFlowCollectionDto())
        val ethCollections = listOf(randomEthCollectionDto(), randomEthCollectionDto(), randomEthCollectionDto())

        coEvery {
            testFlowCollectionApi.searchNftAllCollections(continuation, size)
        } returns FlowNftCollectionsDto(2, null, flowCollections).toMono()

        coEvery {
            testEthereumCollectionApi.searchNftAllCollections(continuation, size)
        } returns NftCollectionsDto(3, null, ethCollections).toMono()

        val unionCollections = collectionControllerClient.getAllCollections(
            blockchains, continuation, size
        ).awaitFirst()

        assertThat(unionCollections.collections).hasSize(5)
        assertThat(unionCollections.total).isEqualTo(5)
        assertThat(unionCollections.continuation).isNull()
    }
}
