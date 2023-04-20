package com.rarible.protocol.union.search.indexer.repository

import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.elastic.EsCollectionGenericFilter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.test.data.info
import com.rarible.protocol.union.enrichment.test.data.randomEsCollection
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
class EsCollectionQueryBuilderServiceIntegrationTest {

    @Autowired
    protected lateinit var repository: EsCollectionRepository

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        elasticsearchTestBootstrapper.bootstrap()
    }

    @Test
    fun `should query with empty filter`() = runBlocking<Unit> {
        // given
        val filter = EsCollectionGenericFilter()
        val toFind1 = randomEsCollection()
        val toFind2 = randomEsCollection()
        repository.saveAll(listOf(toFind1, toFind2))

        // when
        val result = repository.search(filter, null)

        // then
        assertThat(result).containsExactlyInAnyOrder(toFind1.info, toFind2.info)
    }


    @Test
    fun `should query by blockchain`() = runBlocking<Unit> {
        // given
        val filter = EsCollectionGenericFilter(blockchains = setOf(BlockchainDto.SOLANA, BlockchainDto.TEZOS))
        val toFind1 = randomEsCollection().copy(blockchain = BlockchainDto.SOLANA)
        val toFind2 = randomEsCollection().copy(blockchain = BlockchainDto.TEZOS)
        val toSkip1 = randomEsCollection().copy(blockchain = BlockchainDto.ETHEREUM)
        val toSkip2 = randomEsCollection().copy(blockchain = BlockchainDto.POLYGON)
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, null)

        // then
        assertThat(result).containsExactlyInAnyOrder(toFind1.info, toFind2.info)
    }

    @Test
    fun `should query by owners`() = runBlocking<Unit> {
        // given
        val filter = EsCollectionGenericFilter(owners = setOf("loupa", "poupa"))
        val toFind1 = randomEsCollection().copy(owner = "loupa")
        val toFind2 = randomEsCollection().copy(owner = "poupa")
        val toSkip1 = randomEsCollection().copy(owner = "loupeaux")
        val toSkip2 = randomEsCollection().copy(owner = "poupon")
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, null)

        // then
        assertThat(result).containsExactlyInAnyOrder(toFind1.info, toFind2.info)
    }

    @Test
    fun `should query with cursor`()  = runBlocking<Unit> {
        // given
        val filter = EsCollectionGenericFilter(cursor = "500_100")
        val toFind1 = randomEsCollection().copy(date = Instant.ofEpochMilli(450), salt = 10)
        val toFind2 = randomEsCollection().copy(date = Instant.ofEpochMilli(450), salt = 5)
        val toFind3 = randomEsCollection().copy(date = Instant.ofEpochMilli(400), salt = 15)
        val toSkip1 = randomEsCollection().copy(date = Instant.ofEpochMilli(500), salt = 100)
        val toSkip2 = randomEsCollection().copy(date = Instant.ofEpochMilli(600), salt = 7)
        val toSkip3 = randomEsCollection().copy(date = Instant.ofEpochMilli(700), salt = 20)
        repository.saveAll(listOf(toFind1, toFind2, toFind3, toSkip1, toSkip2, toSkip3).shuffled())

        // when
        val result = repository.search(filter, null)

        // then
        assertThat(result).containsExactly(toFind1.info, toFind2.info, toFind3.info)
    }

    @Test
    fun `should do compound query`() = runBlocking<Unit> {
        // given
        val filter = EsCollectionGenericFilter(
            blockchains = setOf(BlockchainDto.SOLANA, BlockchainDto.TEZOS),
            owners = setOf("loupa", "poupa"),
            cursor = "60000_1"
        )

        val toFind1 = randomEsCollection().copy(
            collectionId = "ddd",
            blockchain = BlockchainDto.TEZOS,
            owner = "loupa",
            date = Instant.ofEpochSecond(50),
        )
        val toFind2 = randomEsCollection().copy(
            collectionId = "eee",
            blockchain = BlockchainDto.SOLANA,
            owner = "poupa",
            date = Instant.ofEpochSecond(40),
        )
        // out of cursor
        val toSkip1 = randomEsCollection().copy(
            collectionId = "aaa",
            blockchain = BlockchainDto.TEZOS,
            owner = "loupa",
            date = Instant.ofEpochSecond(60),
            )
        // other blockchain
        val toSkip2 = randomEsCollection().copy(
            collectionId = "fff",
            blockchain = BlockchainDto.ETHEREUM,
            owner = "loupa",
            date = Instant.ofEpochSecond(30),

            )
        // other owner
        val toSkip3 = randomEsCollection().copy(
            collectionId = "hhh",
            blockchain = BlockchainDto.TEZOS,
            owner = "loupeaux",
            date = Instant.ofEpochSecond(20),

            )
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2, toSkip3))

        // when
        val result = repository.search(filter, null)

        // then
        assertThat(result).containsExactly(toFind1.info, toFind2.info)
    }
}
