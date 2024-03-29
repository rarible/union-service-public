package com.rarible.protocol.union.api.controller

import com.rarible.protocol.dto.CollectionsByIdRequestDto
import com.rarible.protocol.dto.FlowNftCollectionsDto
import com.rarible.protocol.dto.NftCollectionsDto
import com.rarible.protocol.solana.dto.CollectionsDto
import com.rarible.protocol.union.api.client.CollectionControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.converter.EnrichmentCollectionConverter
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import com.rarible.protocol.union.enrichment.test.data.randomEsCollection
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import com.rarible.protocol.union.integration.flow.converter.FlowCollectionConverter
import com.rarible.protocol.union.integration.flow.data.randomFlowCollectionDto
import com.rarible.protocol.union.integration.solana.converter.SolanaCollectionConverter
import com.rarible.protocol.union.integration.solana.data.randomSolanaCollectionDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosCollectionDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.TzktCollectionConverter
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono

@IntegrationTest
class CollectionControllerElasticFt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var collectionControllerApi: CollectionControllerApi

    @Autowired
    private lateinit var esCollectionRepository: EsCollectionRepository

    @Autowired
    private lateinit var enrichmentCollectionService: EnrichmentCollectionService

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking {
        elasticsearchTestBootstrapper.bootstrap()
    }

    @Test
    fun `get all collections`() = runBlocking<Unit> {
        // given
        val blockchains =
            listOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON, BlockchainDto.FLOW, BlockchainDto.SOLANA)
        val size = 10

        val ethDto = randomEthCollectionDto()
        val polygonDto = randomEthCollectionDto()
        val flowDto = randomFlowCollectionDto()
        val solanaDto = randomSolanaCollectionDto()
        val tezosDto = randomTezosCollectionDto()

        val esEth1 = randomEsCollection().copy(
            collectionId = "${BlockchainDto.ETHEREUM}:${ethDto.id}",
            blockchain = BlockchainDto.ETHEREUM
        )
        val esPolygon1 = randomEsCollection().copy(
            collectionId = "${BlockchainDto.POLYGON}:${polygonDto.id}",
            blockchain = BlockchainDto.POLYGON
        )
        val esFlow1 = randomEsCollection().copy(
            collectionId = "${BlockchainDto.FLOW}:${flowDto.id}",
            blockchain = BlockchainDto.FLOW
        )
        val esSolana1 = randomEsCollection().copy(
            collectionId = "${BlockchainDto.SOLANA}:${solanaDto.address}",
            blockchain = BlockchainDto.SOLANA
        )
        val esTezos1 = randomEsCollection().copy(
            collectionId = "${BlockchainDto.TEZOS}:${tezosDto.address!!}",
            blockchain = BlockchainDto.TEZOS
        )

        esCollectionRepository.saveAll(
            listOf(
                esEth1,
                esPolygon1,
                esFlow1,
                esSolana1,
                esTezos1,
            )
        )

        val unionEth = EthCollectionConverter.convert(ethDto, BlockchainDto.ETHEREUM)
        val unionPolygon = EthCollectionConverter.convert(polygonDto, BlockchainDto.POLYGON)
        val unionFlow = FlowCollectionConverter.convert(flowDto, BlockchainDto.FLOW)
        val unionSolana = SolanaCollectionConverter.convert(solanaDto, BlockchainDto.SOLANA)
        val unionTezos = TzktCollectionConverter.convert(tezosDto, BlockchainDto.TEZOS)

        enrichmentCollectionService.save(EnrichmentCollectionConverter.convert(unionEth))
        enrichmentCollectionService.save(EnrichmentCollectionConverter.convert(unionPolygon))
        enrichmentCollectionService.save(EnrichmentCollectionConverter.convert(unionFlow))
        enrichmentCollectionService.save(EnrichmentCollectionConverter.convert(unionSolana))
        enrichmentCollectionService.save(EnrichmentCollectionConverter.convert(unionTezos))

        // TODO COLLECTION remove it after switching to union data
        coEvery {
            testEthereumCollectionApi.getNftCollectionsByIds(CollectionsByIdRequestDto(listOf(ethDto.id.toString())))
        } returns NftCollectionsDto(
            total = 1, continuation = null, collections = listOf(
                ethDto
            )
        ).toMono()

        coEvery {
            testEthereumCollectionApi.searchNftAllCollections(any(), any())
        } returns NftCollectionsDto(
            total = 1, continuation = null, collections = listOf(
                ethDto
            )
        ).toMono()

        coEvery {
            testPolygonCollectionApi.getNftCollectionsByIds(CollectionsByIdRequestDto(listOf(polygonDto.id.toString())))
        } returns NftCollectionsDto(
            total = 1, continuation = null, collections = listOf(
                polygonDto
            )
        ).toMono()

        coEvery {
            testFlowCollectionApi.searchNftCollectionsByIds((listOf(flowDto.id)))
        } returns FlowNftCollectionsDto(
            continuation = null, data = listOf(flowDto)
        ).toMono()

        coEvery {
            testSolanaCollectionApi.searchCollectionsByIds(
                com.rarible.protocol.solana.dto.CollectionsByIdRequestDto(listOf(solanaDto.address))
            )
        } returns CollectionsDto(
            collections = listOf(solanaDto)
        ).toMono()

        // when
        val unionCollections = collectionControllerApi.getAllCollections(
            blockchains, null, size
        ).awaitFirst()

        // then
        assertThat(unionCollections.collections).hasSize(4)
        assertThat(unionCollections.total).isEqualTo(4)
    }

    @Test
    fun `get collections by owner`() = runBlocking<Unit> {
        // given
        val blockchains = BlockchainDto.values().toList()
        val size = 10

        val ethDto = randomEthCollectionDto()
        val polygonDto = randomEthCollectionDto()
        val flowDto = randomFlowCollectionDto()
        val tezosDto = randomTezosCollectionDto()
        val owner = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())

        val esEth1 = randomEsCollection().copy(
            collectionId = "${BlockchainDto.ETHEREUM}:${ethDto.id}",
            blockchain = BlockchainDto.ETHEREUM,
            owner = owner.fullId(),
        )
        val esPolygon1 = randomEsCollection().copy(
            collectionId = "${BlockchainDto.POLYGON}:${polygonDto.id}",
            blockchain = BlockchainDto.POLYGON,
            owner = owner.fullId(),
        )
        val esFlow1 = randomEsCollection().copy(
            collectionId = "${BlockchainDto.FLOW}:${flowDto.id}",
            blockchain = BlockchainDto.FLOW
        )
        val esTezos1 = randomEsCollection().copy(
            collectionId = "${BlockchainDto.TEZOS}:${tezosDto.address!!}",
            blockchain = BlockchainDto.TEZOS
        )

        esCollectionRepository.saveAll(
            listOf(
                esEth1,
                esPolygon1,
                esFlow1,
                esTezos1,
            )
        )

        val unionEth = EthCollectionConverter.convert(ethDto, BlockchainDto.ETHEREUM)
        val unionPolygon = EthCollectionConverter.convert(polygonDto, BlockchainDto.POLYGON)
        val unionFlow = FlowCollectionConverter.convert(flowDto, BlockchainDto.FLOW)
        val unionTezos = TzktCollectionConverter.convert(tezosDto, BlockchainDto.TEZOS)

        enrichmentCollectionService.save(EnrichmentCollectionConverter.convert(unionEth))
        enrichmentCollectionService.save(EnrichmentCollectionConverter.convert(unionPolygon))
        enrichmentCollectionService.save(EnrichmentCollectionConverter.convert(unionFlow))
        enrichmentCollectionService.save(EnrichmentCollectionConverter.convert(unionTezos))

        // TODO COLLECTION remove it after switching to union data
        coEvery {
            testEthereumCollectionApi.getNftCollectionsByIds(CollectionsByIdRequestDto(listOf(ethDto.id.toString())))
        } returns NftCollectionsDto(
            total = 1, continuation = null, collections = listOf(
                ethDto
            )
        ).toMono()

        coEvery {
            testPolygonCollectionApi.getNftCollectionsByIds(CollectionsByIdRequestDto(listOf(polygonDto.id.toString())))
        } returns NftCollectionsDto(
            total = 1, continuation = null, collections = listOf(
                polygonDto
            )
        ).toMono()

        // when
        val unionCollections = collectionControllerApi.getCollectionsByOwner(
            owner.fullId(),
            blockchains, null, size
        ).awaitFirst()

        // then
        assertThat(unionCollections.collections).hasSize(2)
        assertThat(unionCollections.total).isEqualTo(2)
    }
}
