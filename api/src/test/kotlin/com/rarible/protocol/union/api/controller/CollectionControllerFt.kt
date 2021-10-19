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
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.test.data.randomEthAddress
import com.rarible.protocol.union.test.data.randomEthCollectionDto
import com.rarible.protocol.union.test.data.randomFlowAddress
import com.rarible.protocol.union.test.data.randomFlowCollectionDto
import com.rarible.protocol.union.test.data.randomTezosCollectionDto
import com.rarible.protocol.union.test.data.randomTezosOwnershipId
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

        assertThat(unionCollection.id.value).isEqualTo(collectionIdFull.value)
        assertThat(unionCollection.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
    }

    @Test
    fun `get collection by id - tezos`() = runBlocking<Unit> {
        val collectionId = randomString()
        val collectionIdFull = UnionAddressConverter.convert(collectionId, BlockchainDto.TEZOS)
        val collection = randomTezosCollectionDto(collectionId)

        coEvery { testTezosCollectionApi.getNftCollectionById(collectionIdFull.value) } returns collection.toMono()

        val unionCollection = collectionControllerClient.getCollectionById(collectionIdFull.fullId()).awaitFirst()

        assertThat(unionCollection.id.value).isEqualTo(collectionIdFull.value)
        assertThat(unionCollection.id.blockchain).isEqualTo(BlockchainDto.TEZOS)
    }

    @Test
    fun `get collection by id - flow`() = runBlocking<Unit> {
        val collectionId = randomString()
        val collectionIdFull = FlowContractConverter.convert(collectionId, BlockchainDto.FLOW)
        val collection = randomFlowCollectionDto(collectionId)

        coEvery { testFlowCollectionApi.getNftCollectionById(collectionId) } returns collection.toMono()

        val unionCollection = collectionControllerClient.getCollectionById(collectionIdFull.fullId()).awaitFirst()
        val flowCollection = unionCollection as CollectionDto

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

        val ethCollection = unionCollections.collections[0]
        assertThat(ethCollection.id.value).isEqualTo(collectionId.value)
    }

    @Test
    fun `get collections by owner - tezos`() = runBlocking<Unit> {
        val tezosOwnerId = randomTezosOwnershipId()
        val collection = randomTezosCollectionDto()
        val collectionId = UnionAddressConverter.convert(collection.id, BlockchainDto.TEZOS)

        coEvery {
            testTezosCollectionApi.searchNftCollectionsByOwner(tezosOwnerId.value, size, continuation)
        } returns com.rarible.protocol.tezos.dto.NftCollectionsDto(1, null, listOf(collection)).toMono()

        val unionCollections = collectionControllerClient.getCollectionsByOwner(
            tezosOwnerId.fullId(), continuation, size
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
        } returns FlowNftCollectionsDto(1, null, listOf(collection)).toMono()

        val unionCollections = collectionControllerClient.getCollectionsByOwner(
            flowOwnerId.fullId(), continuation, size
        ).awaitFirst()

        val flowCollection = unionCollections.collections[0]
        assertThat(flowCollection.id.value).isEqualTo(collection.id)
    }

    @Test
    fun `get all collections`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW, BlockchainDto.TEZOS)
        val continuation = "${nowMillis()}_${randomString()}"
        val size = 10

        val flowCollections = listOf(randomFlowCollectionDto(), randomFlowCollectionDto())
        val ethCollections = listOf(randomEthCollectionDto(), randomEthCollectionDto(), randomEthCollectionDto())
        val tezosCollections = listOf(randomTezosCollectionDto())

        coEvery {
            testFlowCollectionApi.searchNftAllCollections(continuation, size)
        } returns FlowNftCollectionsDto(2, null, flowCollections).toMono()

        coEvery {
            testEthereumCollectionApi.searchNftAllCollections(continuation, size)
        } returns NftCollectionsDto(3, null, ethCollections).toMono()

        coEvery {
            testTezosCollectionApi.searchNftAllCollections(size, continuation)
        } returns com.rarible.protocol.tezos.dto.NftCollectionsDto(1, null, tezosCollections).toMono()

        val unionCollections = collectionControllerClient.getAllCollections(
            blockchains, continuation, size
        ).awaitFirst()

        assertThat(unionCollections.collections).hasSize(6)
        assertThat(unionCollections.total).isEqualTo(6)
        assertThat(unionCollections.continuation).isNull()
    }
}
