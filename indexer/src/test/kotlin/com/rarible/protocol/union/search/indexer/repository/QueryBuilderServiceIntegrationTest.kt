package com.rarible.protocol.union.search.indexer.repository

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsActivitySort
import com.rarible.protocol.union.core.model.ElasticActivityQueryGenericFilter
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.test.data.randomEsActivity
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
@Disabled
internal class QueryBuilderServiceIntegrationTest {

    @Autowired
    protected lateinit var repository: EsActivityRepository

    @Autowired
    private lateinit var esOperations: ReactiveElasticsearchOperations
    
    private val sort = EsActivitySort(latestFirst = true)

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        repository.deleteAll()
    }

    @Test
    fun `should query with empty filter`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter()
        val toFind1 = randomEsActivity()
        val toFind2 = randomEsActivity()
        repository.saveAll(listOf(toFind1, toFind2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by blockchain`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(blockchains = setOf(BlockchainDto.SOLANA, BlockchainDto.TEZOS))
        val toFind1 = randomEsActivity().copy(blockchain = BlockchainDto.SOLANA)
        val toFind2 = randomEsActivity().copy(blockchain = BlockchainDto.TEZOS)
        val toSkip1 = randomEsActivity().copy(blockchain = BlockchainDto.ETHEREUM)
        val toSkip2 = randomEsActivity().copy(blockchain = BlockchainDto.POLYGON)
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by activity type`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(activityTypes = setOf(ActivityTypeDto.SELL, ActivityTypeDto.BURN))
        val toFind1 = randomEsActivity().copy(type = ActivityTypeDto.SELL)
        val toFind2 = randomEsActivity().copy(type = ActivityTypeDto.BURN)
        val toSkip1 = randomEsActivity().copy(type = ActivityTypeDto.BID)
        val toSkip2 = randomEsActivity().copy(type = ActivityTypeDto.MINT)
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by any users`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(anyUsers = setOf("loupa", "poupa"))
        val toFind1 = randomEsActivity().copy(user = EsActivity.User(maker = "loupa", taker = null))
        val toFind2 = randomEsActivity().copy(user = EsActivity.User(maker = "0x00", taker = "poupa"))
        val toSkip1 = randomEsActivity().copy(user = EsActivity.User(maker = "0x01", taker = "0x00"))
        val toSkip2 = randomEsActivity().copy(user = EsActivity.User(maker = "0x03", taker = null))
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by makers`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(makers = setOf("loupa", "poupa"))
        val toFind1 = randomEsActivity().copy(user = EsActivity.User(maker = "loupa", taker = null))
        val toFind2 = randomEsActivity().copy(user = EsActivity.User(maker = "poupa", taker = "0x02"))
        val toSkip1 = randomEsActivity().copy(user = EsActivity.User(maker = "0x01", taker = "loupa"))
        val toSkip2 = randomEsActivity().copy(user = EsActivity.User(maker = "0x03", taker = "poupa"))
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by takers`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(takers = setOf("loupa", "poupa"))
        val toFind1 = randomEsActivity().copy(user = EsActivity.User(maker = "0x01", taker = "loupa"))
        val toFind2 = randomEsActivity().copy(user = EsActivity.User(maker = "0x03", taker = "poupa"))
        val toSkip1 = randomEsActivity().copy(user = EsActivity.User(maker = "loupa", taker = null))
        val toSkip2 = randomEsActivity().copy(user = EsActivity.User(maker = "poupa", taker = "0x02"))
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by any collections`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(anyCollections = setOf("boredApes", "cryptoPunks"))
        val toFind1 = randomEsActivity().copy(collection = EsActivity.Collection(make = "boredApes", take = null))
        val toFind2 = randomEsActivity().copy(collection = EsActivity.Collection(make = "ethDomains", take = "cryptoPunks"))
        val toSkip1 = randomEsActivity().copy(collection = EsActivity.Collection(make = "cryptoKitties", take = null))
        val toSkip2 = randomEsActivity().copy(collection = EsActivity.Collection(make = "cyberBrokers", take = "ethDomains"))
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by make collections`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(makeCollections = setOf("boredApes", "cryptoPunks"))
        val toFind1 = randomEsActivity().copy(collection = EsActivity.Collection(make = "boredApes", take = null))
        val toFind2 = randomEsActivity().copy(collection = EsActivity.Collection(make = "cryptoPunks", take = "ethDomains"))
        val toSkip1 = randomEsActivity().copy(collection = EsActivity.Collection(make = "cryptoKitties", take = "cryptoPunks"))
        val toSkip2 = randomEsActivity().copy(collection = EsActivity.Collection(make = "cyberBrokers", take = "boredApes"))
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by take collections`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(takeCollections = setOf("boredApes", "cryptoPunks"))
        val toFind1 = randomEsActivity().copy(collection = EsActivity.Collection(make = "cryptoKitties", take = "cryptoPunks"))
        val toFind2 = randomEsActivity().copy(collection = EsActivity.Collection(make = "cyberBrokers", take = "boredApes"))
        val toSkip1 = randomEsActivity().copy(collection = EsActivity.Collection(make = "boredApes", take = null))
        val toSkip2 = randomEsActivity().copy(collection = EsActivity.Collection(make = "cryptoPunks", take = "ethDomains"))

        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by any item`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(anyItem = "0x01:111")
        val toFind1 = randomEsActivity().copy(item = EsActivity.Item(make = "0x01:111", take = null))
        val toFind2 = randomEsActivity().copy(item = EsActivity.Item(make = "0x03:333", take = "0x01:111"))
        val toSkip1 = randomEsActivity().copy(item = EsActivity.Item(make = "0x05:555", take = null))
        val toSkip2 = randomEsActivity().copy(item = EsActivity.Item(make = "0x04:444", take = "0x06:666"))

        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by make item`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(makeItem = "0x01:111")
        val toFind1 = randomEsActivity().copy(item = EsActivity.Item(make = "0x01:111", take = null))
        val toSkip0 = randomEsActivity().copy(item = EsActivity.Item(make = "0x03:333", take = "0x01:111"))
        val toSkip1 = randomEsActivity().copy(item = EsActivity.Item(make = "0x05:555", take = null))
        val toSkip2 = randomEsActivity().copy(item = EsActivity.Item(make = "0x04:444", take = "0x06:666"))

        repository.saveAll(listOf(toFind1, toSkip0, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1)
    }

    @Test
    fun `should query by take item`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(takeItem = "0x01:111")
        val toSkip0 = randomEsActivity().copy(item = EsActivity.Item(make = "0x01:111", take = null))
        val toFind1 = randomEsActivity().copy(item = EsActivity.Item(make = "0x03:333", take = "0x01:111"))
        val toSkip1 = randomEsActivity().copy(item = EsActivity.Item(make = "0x05:555", take = null))
        val toSkip2 = randomEsActivity().copy(item = EsActivity.Item(make = "0x04:444", take = "0x06:666"))

        repository.saveAll(listOf(toFind1, toSkip0, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1)
    }

    @Test
    fun `should query by from date`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(from = Instant.ofEpochMilli(500))
        val toFind1 = randomEsActivity().copy(date = Instant.ofEpochMilli(500))
        val toFind2 = randomEsActivity().copy(date = Instant.ofEpochMilli(600))
        val toSkip1 = randomEsActivity().copy(date = Instant.ofEpochMilli(400))
        val toSkip2 = randomEsActivity().copy(date = Instant.ofEpochMilli(300))
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by to date`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(to = Instant.ofEpochMilli(500))
        val toFind1 = randomEsActivity().copy(date = Instant.ofEpochMilli(400))
        val toFind2 = randomEsActivity().copy(date = Instant.ofEpochMilli(500))
        val toSkip1 = randomEsActivity().copy(date = Instant.ofEpochMilli(600))
        val toSkip2 = randomEsActivity().copy(date = Instant.ofEpochMilli(700))
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query between from and to`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(from = Instant.ofEpochMilli(500), to = Instant.ofEpochMilli(1000))
        val toFind1 = randomEsActivity().copy(date = Instant.ofEpochMilli(500))
        val toFind2 = randomEsActivity().copy(date = Instant.ofEpochMilli(1000))
        val toSkip1 = randomEsActivity().copy(date = Instant.ofEpochMilli(250))
        val toSkip2 = randomEsActivity().copy(date = Instant.ofEpochMilli(2000))
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should do compound query`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(
            blockchains = setOf(BlockchainDto.SOLANA, BlockchainDto.TEZOS),
            activityTypes = setOf(ActivityTypeDto.SELL, ActivityTypeDto.BURN),
            anyUsers = setOf("loupa", "poupa"),
            anyCollections = setOf("boredApes", "cryptoPunks"),
            from = Instant.ofEpochMilli(500),
            to = Instant.ofEpochMilli(1000),
        )
        val toFind1 = randomEsActivity().copy(
            blockchain = BlockchainDto.SOLANA,
            type = ActivityTypeDto.BURN,
            user = EsActivity.User(maker = "loupa", taker = null),
            collection = EsActivity.Collection(make = "boredApes", take = null),
            date = Instant.ofEpochMilli(600),
        )
        val toFind2 = randomEsActivity().copy(
            blockchain = BlockchainDto.TEZOS,
            type = ActivityTypeDto.SELL,
            user = EsActivity.User(maker = "0x00", taker = "poupa"),
            collection = EsActivity.Collection(make = "ethDomains", take = "cryptoPunks"),
            date = Instant.ofEpochMilli(900),
        )
        val toSkip1 = randomEsActivity().copy(
            blockchain = BlockchainDto.SOLANA,
            type = ActivityTypeDto.CANCEL_BID,
            user = EsActivity.User(maker = "loupa", taker = null),
            collection = EsActivity.Collection(make = "ethDomains", take = "cryptoPunks"),
            date = Instant.ofEpochMilli(600),
        )
        val toSkip2 = randomEsActivity().copy(
            blockchain = BlockchainDto.ETHEREUM,
            type = ActivityTypeDto.SELL,
            user = EsActivity.User(maker = "0x00", taker = "poupa"),
            collection = EsActivity.Collection(make = "boredApes", take = null),
            date = Instant.ofEpochMilli(250),
        )
        val toSkip3 = randomEsActivity().copy(
            blockchain = BlockchainDto.ETHEREUM,
            type = ActivityTypeDto.CANCEL_LIST,
            user = EsActivity.User(maker = "loupa", taker = "poupa"),
            collection = EsActivity.Collection(make = "cryptoKitties", take = "cryptoPunks"),
            date = Instant.ofEpochMilli(1500),
        )
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2, toSkip3))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }
}
