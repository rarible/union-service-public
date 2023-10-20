package com.rarible.protocol.union.search.indexer.repository

import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.elastic.ElasticActivityFilter
import com.rarible.protocol.union.core.model.elastic.EsActivitySort
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.test.data.randomEsActivity
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.test.context.ContextConfiguration
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.time.Instant

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class EsActivityQueryBuilderServiceIntegrationTest {

    @Autowired
    protected lateinit var repository: EsActivityRepository

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    private val sort = EsActivitySort(latestFirst = true)

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        elasticsearchTestBootstrapper.bootstrap()
    }

    @Test
    fun `should query with empty filter`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityFilter()
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
        val filter = ElasticActivityFilter(blockchains = setOf(BlockchainDto.SOLANA, BlockchainDto.TEZOS))
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
        val filter =
            ElasticActivityFilter(activityTypes = setOf(ActivityTypeDto.SELL, ActivityTypeDto.BURN))
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
        val filter = ElasticActivityFilter(anyUsers = setOf("loupa", "poupa"))
        val toFind1 = randomEsActivity().copy(userFrom = "loupa", userTo = null)
        val toFind2 = randomEsActivity().copy(userFrom = "0x00", userTo = "poupa")
        val toSkip1 = randomEsActivity().copy(userFrom = "0x01", userTo = "0x00")
        val toSkip2 = randomEsActivity().copy(userFrom = "0x03", userTo = null)
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by userFrom`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityFilter(usersFrom = setOf("loupa", "poupa"))
        val toFind1 = randomEsActivity().copy(userFrom = "loupa", userTo = null)
        val toFind2 = randomEsActivity().copy(userFrom = "poupa", userTo = "0x02")
        val toSkip1 = randomEsActivity().copy(userFrom = "0x01", userTo = "loupa")
        val toSkip2 = randomEsActivity().copy(userFrom = "0x03", userTo = "poupa")
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by userTo`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityFilter(usersTo = setOf("loupa", "poupa"))
        val toFind1 = randomEsActivity().copy(userFrom = "0x01", userTo = "loupa")
        val toFind2 = randomEsActivity().copy(userFrom = "0x03", userTo = "poupa")
        val toSkip1 = randomEsActivity().copy(userFrom = "loupa", userTo = null)
        val toSkip2 = randomEsActivity().copy(userFrom = "poupa", userTo = "0x02")
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by collections`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityFilter(
            collections = setOf(
                CollectionIdDto(blockchain = BlockchainDto.ETHEREUM, value = "boredApes"),
                CollectionIdDto(blockchain = BlockchainDto.ETHEREUM, value = "cryptoPunks")
            )
        )
        val toFind1 = randomEsActivity().copy(collection = "boredApes", blockchain = BlockchainDto.ETHEREUM)
        val toFind2 = randomEsActivity().copy(collection = "cryptoPunks", blockchain = BlockchainDto.ETHEREUM)
        val toSkip1 = randomEsActivity().copy(collection = "cryptoKitties", blockchain = BlockchainDto.ETHEREUM)
        val toSkip2 = randomEsActivity().copy(collection = "cyberBrokers", blockchain = BlockchainDto.ETHEREUM)
        val toSkip3 = randomEsActivity().copy(collection = "cryptoPunks", blockchain = BlockchainDto.FLOW)
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2, toSkip3))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by any item`() = runBlocking<Unit> {
        // given
        val toFind1 = randomEsActivity().copy(blockchain = BlockchainDto.ETHEREUM, item = "0x01:111")
        val toFind2 = randomEsActivity().copy(blockchain = BlockchainDto.ETHEREUM, item = "0x01:111")
        val toFind3 = randomEsActivity().copy(blockchain = BlockchainDto.POLYGON, item = "0x01:111")
        val toSkip1 = randomEsActivity().copy(blockchain = BlockchainDto.ETHEREUM, item = "0x05:555")
        val toSkip2 = randomEsActivity().copy(blockchain = BlockchainDto.POLYGON, item = "0x06:666")
        val filter = ElasticActivityFilter(items = setOf(ItemIdDto(BlockchainDto.ETHEREUM, "0x01:111")))

        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should query by from date`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityFilter(from = Instant.ofEpochMilli(500))
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
        val filter = ElasticActivityFilter(to = Instant.ofEpochMilli(500))
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
        val filter =
            ElasticActivityFilter(from = Instant.ofEpochMilli(500), to = Instant.ofEpochMilli(1000))
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
        val filter = ElasticActivityFilter(
            blockchains = setOf(BlockchainDto.SOLANA, BlockchainDto.TEZOS),
            activityTypes = setOf(ActivityTypeDto.SELL, ActivityTypeDto.BURN),
            anyUsers = setOf("loupa", "poupa"),
            collections = setOf(
                CollectionIdDto(blockchain = BlockchainDto.SOLANA, value = "boredApes"),
                CollectionIdDto(blockchain = BlockchainDto.TEZOS, value = "cryptoPunks")
            ),
            from = Instant.ofEpochMilli(500),
            to = Instant.ofEpochMilli(1000),
        )
        val toFind1 = randomEsActivity().copy(
            blockchain = BlockchainDto.SOLANA,
            type = ActivityTypeDto.BURN,
            userFrom = "loupa",
            userTo = null,
            collection = "boredApes",
            date = Instant.ofEpochMilli(600),
        )
        val toFind2 = randomEsActivity().copy(
            blockchain = BlockchainDto.TEZOS,
            type = ActivityTypeDto.SELL,
            userFrom = "0x00",
            userTo = "poupa",
            collection = "cryptoPunks",
            date = Instant.ofEpochMilli(900),
        )
        val toSkip1 = randomEsActivity().copy(
            blockchain = BlockchainDto.SOLANA,
            type = ActivityTypeDto.CANCEL_BID,
            userFrom = "loupa",
            userTo = null,
            collection = "ethDomains",
            date = Instant.ofEpochMilli(600),
        )
        val toSkip2 = randomEsActivity().copy(
            blockchain = BlockchainDto.SOLANA,
            type = ActivityTypeDto.SELL,
            userFrom = "0x00",
            userTo = "poupa",
            collection = "boredApes",
            date = Instant.ofEpochMilli(250),
        )
        val toSkip3 = randomEsActivity().copy(
            blockchain = BlockchainDto.ETHEREUM,
            type = ActivityTypeDto.CANCEL_LIST,
            userFrom = "loupa", userTo = "poupa",
            collection = "cryptoPunks",
            date = Instant.ofEpochMilli(1500),
        )
        repository.saveAll(listOf(toFind1, toFind2, toSkip1, toSkip2, toSkip3))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2)
    }

    @Test
    fun `should do by bid currencies query`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityFilter(
            bidCurrencies = setOf(
                CurrencyIdDto(blockchain = BlockchainDto.ETHEREUM, contract = Address.ONE().toString(), tokenId = null),
                CurrencyIdDto(blockchain = BlockchainDto.ETHEREUM, contract = Address.TWO().toString(), tokenId = null),
            )
        )
        val toFind1 = randomEsActivity().copy(
            blockchain = BlockchainDto.ETHEREUM,
            type = ActivityTypeDto.BID,
            userFrom = "loupa",
            userTo = null,
            collection = "boredApes",
            currency = Address.ONE().toString(),
            date = Instant.ofEpochMilli(600),
        )
        val toFind2 = randomEsActivity().copy(
            blockchain = BlockchainDto.ETHEREUM,
            type = ActivityTypeDto.CANCEL_BID,
            userFrom = "0x00",
            userTo = null,
            collection = "cryptoPunks",
            currency = Address.TWO().toString(),
            date = Instant.ofEpochMilli(900),
        )
        val toFind3 = randomEsActivity().copy(
            blockchain = BlockchainDto.SOLANA,
            type = ActivityTypeDto.LIST,
            userFrom = "loupa",
            userTo = null,
            collection = "boredApes",
            currency = AddressFactory.create().toString(),
            date = Instant.ofEpochMilli(600),
        )
        val toSkip1 = randomEsActivity().copy(
            blockchain = BlockchainDto.SOLANA,
            type = ActivityTypeDto.CANCEL_BID,
            userFrom = "loupa",
            userTo = null,
            currency = AddressFactory.create().toString(),
            collection = "ethDomains",
            date = Instant.ofEpochMilli(600),
        )
        val toSkip2 = randomEsActivity().copy(
            blockchain = BlockchainDto.SOLANA,
            type = ActivityTypeDto.BID,
            userFrom = "0x00",
            userTo = null,
            collection = "boredApes",
            currency = AddressFactory.create().toString(),
            date = Instant.ofEpochMilli(250),
        )
        repository.saveAll(listOf(toFind1, toFind2, toFind3, toSkip1, toSkip2))

        // when
        val result = repository.search(filter, sort, null)

        // then
        assertThat(result.activities).containsExactlyInAnyOrder(toFind1, toFind2, toFind3)
    }
}
