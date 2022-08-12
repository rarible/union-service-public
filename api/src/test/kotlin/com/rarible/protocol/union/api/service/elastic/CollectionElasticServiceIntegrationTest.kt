package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.test.data.randomEsCollection
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.solana.data.randomSolanaCollectionDto
import com.rarible.protocol.union.integration.solana.data.randomSolanaTokenAddress
import io.mockk.every
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address


@IntegrationTest
class CollectionElasticServiceIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var repository: EsCollectionRepository

    @Autowired
    private lateinit var service: CollectionElasticService

    @Test
    @Disabled("Works locally, fix under PT-953")
    fun `should get all collections`() = runBlocking<Unit> {
        // given
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.SOLANA)
        val continuation = null
        val size = 10
        val col1address = randomEthCollectionId()
        val col1 = randomEsCollection()
            .copy(collectionId = col1address.fullId(), blockchain = BlockchainDto.ETHEREUM)
        val col2address = randomSolanaTokenAddress()
        val ethCol1 = randomEthCollectionDto()
            .copy(id = Address.apply(col1address.value))
        val col2 = randomEsCollection()
            .copy(collectionId = "SOLANA:$col2address", blockchain = BlockchainDto.SOLANA)
        val solanaCol2 = randomSolanaCollectionDto()
            .copy(address = col2address)
        repository.saveAll(listOf(col1, col2))
        every {
            testSolanaCollectionApi.searchCollectionsByIds(
                com.rarible.protocol.solana.dto.CollectionsByIdRequestDto(listOf(col2address))
            )
        } returns com.rarible.protocol.solana.dto.CollectionsDto(listOf(solanaCol2)).toMono()
        every {
            testEthereumCollectionApi.getNftCollectionsByIds(
                com.rarible.protocol.dto.CollectionsByIdRequestDto(listOf(col1address.value))
            )
        } returns com.rarible.protocol.dto.NftCollectionsDto(collections = listOf(ethCol1)).toMono()

        // when
        val actual = service.getAllCollections(blockchains, continuation, size)

        // then
        assertThat(actual.collections).hasSize(2)
        val solana = actual.collections.find { it.blockchain == BlockchainDto.SOLANA }!!
        assertThat(solana.id.fullId()).isEqualTo("SOLANA:$col2address")
        val eth = actual.collections.find { it.blockchain == BlockchainDto.ETHEREUM }!!
        assertThat(eth.id).isEqualTo(col1address)
    }
}
