package com.rarible.protocol.union.search.indexer.repository

import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsActivityCursor
import com.rarible.protocol.union.core.model.EsActivitySort
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.repository.search.internal.EsQueryCursorService
import com.rarible.protocol.union.enrichment.test.data.randomEsActivity
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.index.query.BoolQueryBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
@Disabled
internal class QueryCursorServiceIntegrationTest {

    @Autowired
    protected lateinit var repository: EsActivityRepository

    @Autowired
    private lateinit var esOperations: ReactiveElasticsearchOperations

    @Autowired
    private lateinit var service: EsQueryCursorService

    @BeforeEach
    fun setUp() = runBlocking {
        repository.deleteAll()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should query with cursor - different date`(latestFirst: Boolean) = runBlocking<Unit> {
        // given
        val builder = NativeSearchQueryBuilder()
        val boolQuery = BoolQueryBuilder()
        val gte1 = randomEsActivity().copy(date = Instant.ofEpochMilli(2000))
        val lte1 = randomEsActivity().copy(date = Instant.ofEpochMilli(1000))
        val cursor = EsActivityCursor(
            date = Instant.ofEpochMilli(1500),
            blockNumber = null,
            logIndex = null,
            salt = 0,
        )
        repository.saveAll(listOf(gte1, lte1))

        // when
        service.applyCursor(boolQuery, EsActivitySort(latestFirst), cursor.toString())
        builder.withQuery(boolQuery)
        val searchHits = esOperations.search(builder.build(), EsActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        if (latestFirst) {
            assertThat(searchHits).containsExactlyInAnyOrder(lte1)
        } else {
            assertThat(searchHits).containsExactlyInAnyOrder(gte1)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should query with cursor - same date, different blockNumber`(latestFirst: Boolean) = runBlocking<Unit> {
        // given
        val builder = NativeSearchQueryBuilder()
        val boolQuery = BoolQueryBuilder()
        val gte1 = randomEsActivity().copy(
            date = Instant.ofEpochMilli(2000),
            blockNumber = 60,
        )
        val gte2 = randomEsActivity().copy(
            date = Instant.ofEpochMilli(3000),
            blockNumber = null,
        )
        val lte1 = randomEsActivity().copy(
            date = Instant.ofEpochMilli(1000),
            blockNumber = 55,
        )
        val lte2 = randomEsActivity().copy(
            date = Instant.ofEpochMilli(2000),
            blockNumber = 40,
        )

        val cursor = EsActivityCursor(
            date = Instant.ofEpochMilli(2000),
            blockNumber = 50,
            logIndex = null,
            salt = 0,
        )
        repository.saveAll(listOf(gte1, gte2, lte1, lte2))

        // when
        service.applyCursor(boolQuery, EsActivitySort(latestFirst), cursor.toString())
        builder.withQuery(boolQuery)
        val searchHits = esOperations.search(builder.build(), EsActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        if (latestFirst) {
            assertThat(searchHits).containsExactlyInAnyOrder(lte1, lte2)
        } else {
            assertThat(searchHits).containsExactlyInAnyOrder(gte1, gte2)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should query with cursor - same date, blockNumber, different logIndex`(latestFirst: Boolean) = runBlocking<Unit> {
        // given
        val builder = NativeSearchQueryBuilder()
        val boolQuery = BoolQueryBuilder()
        val gte1 = randomEsActivity().copy(
            date = Instant.ofEpochMilli(2000),
            blockNumber = 60,
            logIndex = 3,
        )
        val gte2 = randomEsActivity().copy(
            date = Instant.ofEpochMilli(2000),
            blockNumber = 50,
            logIndex = 6,
        )
        val lte1 = randomEsActivity().copy(
            date = Instant.ofEpochMilli(2000),
            blockNumber = 50,
            logIndex = 4,
        )
        val lte2 = randomEsActivity().copy(
            date = Instant.ofEpochMilli(2000),
            blockNumber = 40,
            logIndex = 7,
        )

        val cursor = EsActivityCursor(
            date = Instant.ofEpochMilli(2000),
            blockNumber = 50,
            logIndex = 5,
            salt = 0,
        )
        repository.saveAll(listOf(gte1, gte2, lte1, lte2))

        // when
        service.applyCursor(boolQuery, EsActivitySort(latestFirst), cursor.toString())
        builder.withQuery(boolQuery)
        val searchHits = esOperations.search(builder.build(), EsActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        if (latestFirst) {
            assertThat(searchHits).containsExactlyInAnyOrder(lte1, lte2)
        } else {
            assertThat(searchHits).containsExactlyInAnyOrder(gte1, gte2)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should query with cursor - same date, blockNumber, logIndex`(latestFirst: Boolean) = runBlocking<Unit> {
        // given
        val builder = NativeSearchQueryBuilder()
        val boolQuery = BoolQueryBuilder()
        val gte1 = randomEsActivity().copy(
            date = Instant.ofEpochMilli(2000),
            blockNumber = 50,
            logIndex = 6,
            salt = 50,
        )
        val gte2 = randomEsActivity().copy(
            date = Instant.ofEpochMilli(2000),
            blockNumber = 50,
            logIndex = 5,
            salt = 110,
        )
        val lte1 = randomEsActivity().copy(
            date = Instant.ofEpochMilli(2000),
            blockNumber = 50,
            logIndex = 5,
            salt = 90,
        )
        val lte2 = randomEsActivity().copy(
            date = Instant.ofEpochMilli(2000),
            blockNumber = 50,
            logIndex = 4,
            salt = 150,
        )

        val cursor = EsActivityCursor(
            date = Instant.ofEpochMilli(2000),
            blockNumber = 50,
            logIndex = 5,
            salt = 100,
        )
        repository.saveAll(listOf(gte1, gte2, lte1, lte2))

        // when
        service.applyCursor(boolQuery, EsActivitySort(latestFirst), cursor.toString())
        builder.withQuery(boolQuery)
        val searchHits = esOperations.search(builder.build(), EsActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        if (latestFirst) {
            assertThat(searchHits).containsExactlyInAnyOrder(lte1, lte2)
        } else {
            assertThat(searchHits).containsExactlyInAnyOrder(gte1, gte2)
        }
    }

    @Test
    fun `should skip applying cursor if it is null`() {
        // given
        val emptyBoolQuery = BoolQueryBuilder()
        val boolQuery = BoolQueryBuilder()

        // when
        service.applyCursor(boolQuery, EsActivitySort(false), null)

        // then
        assertThat(boolQuery).isEqualTo(emptyBoolQuery)
    }

    @Test
    fun `should skip applying cursor if it is malformed`() {
        // given
        val emptyBoolQuery = BoolQueryBuilder()
        val boolQuery = BoolQueryBuilder()

        // when
        service.applyCursor(boolQuery, EsActivitySort(false), "some malformed cursor string")

        // then
        assertThat(boolQuery).isEqualTo(emptyBoolQuery)
    }
}
