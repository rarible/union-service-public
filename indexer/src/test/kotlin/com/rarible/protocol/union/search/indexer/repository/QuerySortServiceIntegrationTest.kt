package com.rarible.protocol.union.search.indexer.repository

import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsActivitySort
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.repository.search.internal.EsQuerySortService
import com.rarible.protocol.union.enrichment.test.data.randomEsActivity
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
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
internal class QuerySortServiceIntegrationTest {

    @Autowired
    protected lateinit var repository: EsActivityRepository

    @Autowired
    private lateinit var esOperations: ReactiveElasticsearchOperations

    @Autowired
    private lateinit var service: EsQuerySortService

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        repository.deleteAll()
    }

    @Test
    fun `should sort by date - blockNumber - logIndex, ascending`() = runBlocking<Unit> {
        // given
        val builder = NativeSearchQueryBuilder()
        val first = activityWithSortFields(100, 3000, 60000, 0)
        val second = activityWithSortFields(100, 3200, 50000, 0)
        val third = activityWithSortFields(100, 3200, 53000, 0)
        val fourth = activityWithSortFields(120, 2000, 40000, 0)
        val fifth = activityWithSortFields(120, 2100, 38000, 0)
        val sixth = activityWithSortFields(120, 2100, 39000, 0)
        val seventh = activityWithSortFields(150, 1000, 20000, 40)
        val eighth = activityWithSortFields(150, 1000, 20000, 60)

        repository.saveAll(listOf(second, fourth, sixth, eighth, seventh, fifth, third, first))

        // when
        service.applySort(builder, EsActivitySort(latestFirst = false))
        val searchHits = esOperations.search(builder.build(), EsActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactly(first, second, third, fourth, fifth, sixth, seventh, eighth)
    }

    @Test
    fun `should sort by date - blockNumber - logIndex, descending`() = runBlocking<Unit> {
        // given
        val builder = NativeSearchQueryBuilder()
        val first = activityWithSortFields(100, 3000, 60000, 0)
        val second = activityWithSortFields(100, 3200, 50000, 0)
        val third = activityWithSortFields(100, 3200, 53000, 0)
        val fourth = activityWithSortFields(120, 2000, 40000, 0)
        val fifth = activityWithSortFields(120, 2100, 38000, 0)
        val sixth = activityWithSortFields(120, 2100, 39000, 0)
        val seventh = activityWithSortFields(150, 1000, 20000, 40)
        val eighth = activityWithSortFields(150, 1000, 20000, 60)

        repository.saveAll(listOf(second, fourth, sixth, eighth, seventh, fifth, third, first))

        // when
        service.applySort(builder, EsActivitySort(latestFirst = true))
        val searchHits = esOperations.search(builder.build(), EsActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactly(eighth, seventh, sixth, fifth, fourth, third, second, first)
    }

    private fun activityWithSortFields(dateMillis: Long, blockNumber: Long, logIndex: Int, salt: Long): EsActivity {
        return randomEsActivity().copy(
            date = Instant.ofEpochMilli(dateMillis),
            blockNumber = blockNumber,
            logIndex = logIndex,
            salt = salt,
        )
    }
}
