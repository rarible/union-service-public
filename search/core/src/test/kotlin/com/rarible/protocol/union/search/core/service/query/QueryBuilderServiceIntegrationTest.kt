package com.rarible.protocol.union.search.core.service.query

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.search.core.ElasticActivity
import com.rarible.protocol.union.search.core.config.SearchConfiguration
import com.rarible.protocol.union.search.core.model.ActivitySort
import com.rarible.protocol.union.search.core.model.ElasticActivityQueryGenericFilter
import com.rarible.protocol.union.search.core.repository.ActivityEsRepository
import com.rarible.protocol.union.search.test.IntegrationTest
import com.rarible.protocol.union.search.test.buildActivity
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class QueryBuilderServiceIntegrationTest {

    @Autowired
    protected lateinit var activityEsRepository: ActivityEsRepository

    @Autowired
    protected lateinit var service: QueryBuilderService

    @Autowired
    private lateinit var esOperations: ReactiveElasticsearchOperations
    
    private val sort = ActivitySort(latestFirst = true)

    @BeforeEach
    fun setUp() {
        activityEsRepository.deleteAll().block()
    }

    @Test
    fun `should query with empty filter`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter()
        val toFind1 = buildActivity()
        val toFind2 = buildActivity()
        activityEsRepository.saveAll(listOf(toFind1, toFind2)).awaitLast()

        // when
        val searchQuery = service.build(filter, sort)
        val searchHits = esOperations.search(searchQuery, ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by blockchain`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(blockchains = setOf(BlockchainDto.SOLANA, BlockchainDto.TEZOS))
        val toFind1 = buildActivity().copy(blockchain = BlockchainDto.SOLANA)
        val toFind2 = buildActivity().copy(blockchain = BlockchainDto.TEZOS)
        val toSkip1 = buildActivity().copy(blockchain = BlockchainDto.ETHEREUM)
        val toSkip2 = buildActivity().copy(blockchain = BlockchainDto.POLYGON)
        activityEsRepository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2)).awaitLast()

        // when
        val searchQuery = service.build(filter, sort)
        val searchHits = esOperations.search(searchQuery, ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by activity type`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(activityTypes = setOf(ActivityTypeDto.SELL, ActivityTypeDto.BURN))
        val toFind1 = buildActivity().copy(type = ActivityTypeDto.SELL)
        val toFind2 = buildActivity().copy(type = ActivityTypeDto.BURN)
        val toSkip1 = buildActivity().copy(type = ActivityTypeDto.BID)
        val toSkip2 = buildActivity().copy(type = ActivityTypeDto.MINT)
        activityEsRepository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2)).awaitLast()

        // when
        val searchQuery = service.build(filter, sort)
        val searchHits = esOperations.search(searchQuery, ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by any users`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(anyUsers = setOf("loupa", "poupa"))
        val toFind1 = buildActivity().copy(user = ElasticActivity.User(maker = "loupa", taker = null))
        val toFind2 = buildActivity().copy(user = ElasticActivity.User(maker = "0x00", taker = "poupa"))
        val toSkip1 = buildActivity().copy(user = ElasticActivity.User(maker = "0x01", taker = "0x00"))
        val toSkip2 = buildActivity().copy(user = ElasticActivity.User(maker = "0x03", taker = null))
        activityEsRepository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2)).awaitLast()

        // when
        val searchQuery = service.build(filter, sort)
        val searchHits = esOperations.search(searchQuery, ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by makers`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(makers = setOf("loupa", "poupa"))
        val toFind1 = buildActivity().copy(user = ElasticActivity.User(maker = "loupa", taker = null))
        val toFind2 = buildActivity().copy(user = ElasticActivity.User(maker = "poupa", taker = "0x02"))
        val toSkip1 = buildActivity().copy(user = ElasticActivity.User(maker = "0x01", taker = "loupa"))
        val toSkip2 = buildActivity().copy(user = ElasticActivity.User(maker = "0x03", taker = "poupa"))
        activityEsRepository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2)).awaitLast()

        // when
        val searchQuery = service.build(filter, sort)
        val searchHits = esOperations.search(searchQuery, ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by takers`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(takers = setOf("loupa", "poupa"))
        val toFind1 = buildActivity().copy(user = ElasticActivity.User(maker = "0x01", taker = "loupa"))
        val toFind2 = buildActivity().copy(user = ElasticActivity.User(maker = "0x03", taker = "poupa"))
        val toSkip1 = buildActivity().copy(user = ElasticActivity.User(maker = "loupa", taker = null))
        val toSkip2 = buildActivity().copy(user = ElasticActivity.User(maker = "poupa", taker = "0x02"))
        activityEsRepository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2)).awaitLast()

        // when
        val searchQuery = service.build(filter, sort)
        val searchHits = esOperations.search(searchQuery, ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by any collections`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(anyCollections = setOf("boredApes", "cryptoPunks"))
        val toFind1 = buildActivity().copy(collection = ElasticActivity.Collection(make = "boredApes", take = null))
        val toFind2 = buildActivity().copy(collection = ElasticActivity.Collection(make = "ethDomains", take = "cryptoPunks"))
        val toSkip1 = buildActivity().copy(collection = ElasticActivity.Collection(make = "cryptoKitties", take = null))
        val toSkip2 = buildActivity().copy(collection = ElasticActivity.Collection(make = "cyberBrokers", take = "ethDomains"))
        activityEsRepository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2)).awaitLast()

        // when
        val searchQuery = service.build(filter, sort)
        val searchHits = esOperations.search(searchQuery, ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by make collections`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(makeCollections = setOf("boredApes", "cryptoPunks"))
        val toFind1 = buildActivity().copy(collection = ElasticActivity.Collection(make = "boredApes", take = null))
        val toFind2 = buildActivity().copy(collection = ElasticActivity.Collection(make = "cryptoPunks", take = "ethDomains"))
        val toSkip1 = buildActivity().copy(collection = ElasticActivity.Collection(make = "cryptoKitties", take = "cryptoPunks"))
        val toSkip2 = buildActivity().copy(collection = ElasticActivity.Collection(make = "cyberBrokers", take = "boredApes"))
        activityEsRepository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2)).awaitLast()

        // when
        val searchQuery = service.build(filter, sort)
        val searchHits = esOperations.search(searchQuery, ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by take collections`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(takeCollections = setOf("boredApes", "cryptoPunks"))
        val toFind1 = buildActivity().copy(collection = ElasticActivity.Collection(make = "cryptoKitties", take = "cryptoPunks"))
        val toFind2 = buildActivity().copy(collection = ElasticActivity.Collection(make = "cyberBrokers", take = "boredApes"))
        val toSkip1 = buildActivity().copy(collection = ElasticActivity.Collection(make = "boredApes", take = null))
        val toSkip2 = buildActivity().copy(collection = ElasticActivity.Collection(make = "cryptoPunks", take = "ethDomains"))

        activityEsRepository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2)).awaitLast()

        // when
        val searchQuery = service.build(filter, sort)
        val searchHits = esOperations.search(searchQuery, ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by any items`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(anyItems = setOf("0x01", "0x02"))
        val toFind1 = buildActivity().copy(item = ElasticActivity.Item(make = "0x01", take = null))
        val toFind2 = buildActivity().copy(item = ElasticActivity.Item(make = "0x03", take = "0x02"))
        val toSkip1 = buildActivity().copy(item = ElasticActivity.Item(make = "0x05", take = null))
        val toSkip2 = buildActivity().copy(item = ElasticActivity.Item(make = "0x04", take = "0x06"))

        activityEsRepository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2)).awaitLast()

        // when
        val searchQuery = service.build(filter, sort)
        val searchHits = esOperations.search(searchQuery, ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by make items`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(makeItems = setOf("0x01", "0x02"))
        val toFind1 = buildActivity().copy(item = ElasticActivity.Item(make = "0x01", take = null))
        val toFind2 = buildActivity().copy(item = ElasticActivity.Item(make = "0x02", take = "0x03"))
        val toSkip1 = buildActivity().copy(item = ElasticActivity.Item(make = "0x05", take = "0x01"))
        val toSkip2 = buildActivity().copy(item = ElasticActivity.Item(make = "0x04", take = "0x02"))

        activityEsRepository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2)).awaitLast()

        // when
        val searchQuery = service.build(filter, sort)
        val searchHits = esOperations.search(searchQuery, ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by take items`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(takeItems = setOf("0x01", "0x02"))
        val toFind1 = buildActivity().copy(item = ElasticActivity.Item(make = "0x05", take = "0x01"))
        val toFind2 = buildActivity().copy(item = ElasticActivity.Item(make = "0x04", take = "0x02"))
        val toSkip1 = buildActivity().copy(item = ElasticActivity.Item(make = "0x01", take = null))
        val toSkip2 = buildActivity().copy(item = ElasticActivity.Item(make = "0x02", take = "0x03"))

        activityEsRepository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2)).awaitLast()

        // when
        val searchQuery = service.build(filter, sort)
        val searchHits = esOperations.search(searchQuery, ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by from date`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(from = Instant.ofEpochMilli(500))
        val toFind1 = buildActivity().copy(date = Instant.ofEpochMilli(500))
        val toFind2 = buildActivity().copy(date = Instant.ofEpochMilli(600))
        val toSkip1 = buildActivity().copy(date = Instant.ofEpochMilli(400))
        val toSkip2 = buildActivity().copy(date = Instant.ofEpochMilli(300))
        activityEsRepository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2)).awaitLast()

        // when
        val searchQuery = service.build(filter, sort)
        val searchHits = esOperations.search(searchQuery, ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by to date`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(to = Instant.ofEpochMilli(500))
        val toFind1 = buildActivity().copy(date = Instant.ofEpochMilli(400))
        val toFind2 = buildActivity().copy(date = Instant.ofEpochMilli(500))
        val toSkip1 = buildActivity().copy(date = Instant.ofEpochMilli(600))
        val toSkip2 = buildActivity().copy(date = Instant.ofEpochMilli(700))
        activityEsRepository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2)).awaitLast()

        // when
        val searchQuery = service.build(filter, sort)
        val searchHits = esOperations.search(searchQuery, ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query between from and to`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(from = Instant.ofEpochMilli(500), to = Instant.ofEpochMilli(1000))
        val toFind1 = buildActivity().copy(date = Instant.ofEpochMilli(500))
        val toFind2 = buildActivity().copy(date = Instant.ofEpochMilli(1000))
        val toSkip1 = buildActivity().copy(date = Instant.ofEpochMilli(250))
        val toSkip2 = buildActivity().copy(date = Instant.ofEpochMilli(2000))
        activityEsRepository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2)).awaitLast()

        // when
        val searchQuery = service.build(filter, sort)
        val searchHits = esOperations.search(searchQuery, ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactlyInAnyOrder(toFind1, toFind2)
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
        val toFind1 = buildActivity().copy(
            blockchain = BlockchainDto.SOLANA,
            type = ActivityTypeDto.BURN,
            user = ElasticActivity.User(maker = "loupa", taker = null),
            collection = ElasticActivity.Collection(make = "boredApes", take = null),
            date = Instant.ofEpochMilli(600),
        )
        val toFind2 = buildActivity().copy(
            blockchain = BlockchainDto.TEZOS,
            type = ActivityTypeDto.SELL,
            user = ElasticActivity.User(maker = "0x00", taker = "poupa"),
            collection = ElasticActivity.Collection(make = "ethDomains", take = "cryptoPunks"),
            date = Instant.ofEpochMilli(900),
        )
        val toSkip1 = buildActivity().copy(
            blockchain = BlockchainDto.SOLANA,
            type = ActivityTypeDto.CANCEL_BID,
            user = ElasticActivity.User(maker = "loupa", taker = null),
            collection = ElasticActivity.Collection(make = "ethDomains", take = "cryptoPunks"),
            date = Instant.ofEpochMilli(600),
        )
        val toSkip2 = buildActivity().copy(
            blockchain = BlockchainDto.ETHEREUM,
            type = ActivityTypeDto.SELL,
            user = ElasticActivity.User(maker = "0x00", taker = "poupa"),
            collection = ElasticActivity.Collection(make = "boredApes", take = null),
            date = Instant.ofEpochMilli(250),
        )
        val toSkip3 = buildActivity().copy(
            blockchain = BlockchainDto.ETHEREUM,
            type = ActivityTypeDto.CANCEL_LIST,
            user = ElasticActivity.User(maker = "loupa", taker = "poupa"),
            collection = ElasticActivity.Collection(make = "cryptoKitties", take = "cryptoPunks"),
            date = Instant.ofEpochMilli(1500),
        )
        activityEsRepository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2, toSkip3)).awaitLast()

        // when
        val searchQuery = service.build(filter, sort)
        val searchHits = esOperations.search(searchQuery, ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactlyInAnyOrder(toFind1, toFind2)
    }
}
