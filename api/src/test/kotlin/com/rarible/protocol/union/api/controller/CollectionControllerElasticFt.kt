package com.rarible.protocol.union.api.controller

import com.rarible.protocol.dto.CollectionsByIdRequestDto
import com.rarible.protocol.dto.FlowNftCollectionsDto
import com.rarible.protocol.dto.NftCollectionsDto
import com.rarible.protocol.solana.dto.CollectionsDto
import com.rarible.protocol.union.api.client.CollectionControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.test.data.randomEsCollection
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import com.rarible.protocol.union.integration.flow.data.randomFlowCollectionDto
import com.rarible.protocol.union.integration.solana.data.randomSolanaCollectionDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosCollectionDto
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.kotlin.core.publisher.toMono

@IntegrationTest
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "application.environment = test",
        "spring.cloud.consul.config.enabled = false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "common.feature-flags.enableCollectionQueriesToElastic = true"
    ]
)
class CollectionControllerElasticFt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var collectionControllerApi: CollectionControllerApi

    @Autowired
    private lateinit var repository: EsCollectionRepository

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking {
        elasticsearchTestBootstrapper.bootstrap()
    }

//    @Test
//    fun `get all collections`() = runBlocking<Unit> {
//        // given
//        val blockchains =
//            listOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON, BlockchainDto.FLOW, BlockchainDto.SOLANA)
//        val size = 10
//
//        val ethDto1 = randomEthCollectionDto()
//        val polygonDto1 = randomEthCollectionDto()
//        val flowDto1 = randomFlowCollectionDto()
//        val solanaDto1 = randomSolanaCollectionDto()
//        val tezosDto1 = randomTezosCollectionDto()
//
//        val esEth1 = randomEsCollection().copy(
//            collectionId = "${BlockchainDto.ETHEREUM}:${ethDto1.id}",
//            blockchain = BlockchainDto.ETHEREUM
//        )
//        val esPolygon1 = randomEsCollection().copy(
//            collectionId = "${BlockchainDto.POLYGON}:${polygonDto1.id}",
//            blockchain = BlockchainDto.POLYGON
//        )
//        val esFlow1 = randomEsCollection().copy(
//            collectionId = "${BlockchainDto.FLOW}:${flowDto1.id}",
//            blockchain = BlockchainDto.FLOW
//        )
//        val esSolana1 = randomEsCollection().copy(
//            collectionId = "${BlockchainDto.SOLANA}:${solanaDto1.address}",
//            blockchain = BlockchainDto.SOLANA
//        )
//        val esTezos1 = randomEsCollection().copy(
//            collectionId = "${BlockchainDto.TEZOS}:${tezosDto1.id}",
//            blockchain = BlockchainDto.TEZOS
//        )
//
//        repository.saveAll(
//            listOf(
//                esEth1,
//                esPolygon1,
//                esFlow1,
//                esSolana1,
//                esTezos1,
//            )
//        )
//
//        coEvery {
//            testEthereumCollectionApi.getNftCollectionsByIds(CollectionsByIdRequestDto(listOf(ethDto1.id.toString())))
//        } returns NftCollectionsDto(
//            total = 1, continuation = null, collections = listOf(
//                ethDto1
//            )
//        ).toMono()
//
//        coEvery {
//            testEthereumCollectionApi.searchNftAllCollections(any(), any())
//        } returns NftCollectionsDto(
//            total = 1, continuation = null, collections = listOf(
//                ethDto1
//            )
//        ).toMono()
//
//        coEvery {
//            testPolygonCollectionApi.getNftCollectionsByIds(CollectionsByIdRequestDto(listOf(polygonDto1.id.toString())))
//        } returns NftCollectionsDto(
//            total = 1, continuation = null, collections = listOf(
//                polygonDto1
//            )
//        ).toMono()
//
//        coEvery {
//            testFlowCollectionApi.searchNftCollectionsByIds((listOf(flowDto1.id)))
//        } returns FlowNftCollectionsDto(
//            total = 1, continuation = null, data = listOf(
//                flowDto1
//            )
//        ).toMono()
//
//        coEvery {
//            testSolanaCollectionApi.searchCollectionsByIds(
//                com.rarible.protocol.solana.dto.CollectionsByIdRequestDto(listOf(solanaDto1.address))
//            )
//        } returns CollectionsDto(
//            collections = listOf(solanaDto1)
//        ).toMono()
//
//        // when
//        val unionCollections = collectionControllerApi.getAllCollections(
//            blockchains, null, size
//        ).awaitFirst()
//
//        // then
//        assertThat(unionCollections.collections).hasSize(4)
//        assertThat(unionCollections.total).isEqualTo(4)
//    }

//    @Test
//    fun `get collections by owner`() = runBlocking<Unit> {
//        // given
//        val blockchains = BlockchainDto.values().toList()
//        val size = 10
//
//        val ethDto1 = randomEthCollectionDto()
//        val polygonDto1 = randomEthCollectionDto()
//        val flowDto1 = randomFlowCollectionDto()
//        val tezosDto1 = randomTezosCollectionDto()
//
//        val esEth1 = randomEsCollection().copy(
//            collectionId = "${BlockchainDto.ETHEREUM}:${ethDto1.id}",
//            blockchain = BlockchainDto.ETHEREUM,
//            owner = "0x12345",
//        )
//        val esPolygon1 = randomEsCollection().copy(
//            collectionId = "${BlockchainDto.POLYGON}:${polygonDto1.id}",
//            blockchain = BlockchainDto.POLYGON,
//            owner = "0x12345",
//        )
//        val esFlow1 = randomEsCollection().copy(
//            collectionId = "${BlockchainDto.FLOW}:${flowDto1.id}",
//            blockchain = BlockchainDto.FLOW
//        )
//        val esTezos1 = randomEsCollection().copy(
//            collectionId = "${BlockchainDto.TEZOS}:${tezosDto1.id}",
//            blockchain = BlockchainDto.TEZOS
//        )
//
//        repository.saveAll(
//            listOf(
//                esEth1,
//                esPolygon1,
//                esFlow1,
//                esTezos1,
//            )
//        )
//
//        coEvery {
//            testEthereumCollectionApi.getNftCollectionsByIds(CollectionsByIdRequestDto(listOf(ethDto1.id.toString())))
//        } returns NftCollectionsDto(
//            total = 1, continuation = null, collections = listOf(
//                ethDto1
//            )
//        ).toMono()
//
//        coEvery {
//            testPolygonCollectionApi.getNftCollectionsByIds(CollectionsByIdRequestDto(listOf(polygonDto1.id.toString())))
//        } returns NftCollectionsDto(
//            total = 1, continuation = null, collections = listOf(
//                polygonDto1
//            )
//        ).toMono()
//
//        // when
//        val unionCollections = collectionControllerApi.getCollectionsByOwner(
//            "0x12345",
//            blockchains, null, size
//        ).awaitFirst()
//
//        // then
//        assertThat(unionCollections.collections).hasSize(2)
//        assertThat(unionCollections.total).isEqualTo(2)
//    }
}
