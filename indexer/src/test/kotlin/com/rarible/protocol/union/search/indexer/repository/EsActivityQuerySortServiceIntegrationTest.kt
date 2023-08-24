package com.rarible.protocol.union.search.indexer.repository

import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.elastic.EsActivity
import com.rarible.protocol.union.core.model.elastic.EsActivitySort
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.repository.search.internal.EsActivityQuerySortService
import com.rarible.protocol.union.enrichment.test.data.randomEsActivity
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class EsActivityQuerySortServiceIntegrationTest {

    @Autowired
    protected lateinit var repository: EsActivityRepository

    @Autowired
    private lateinit var service: EsActivityQuerySortService

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        elasticsearchTestBootstrapper.bootstrap()
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
        val actual = repository.search(builder.build())

        // then
        assertThat(actual.activities).containsExactly(first, second, third, fourth, fifth, sixth, seventh, eighth)
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
        val actual = repository.search(builder.build())

        // then
        assertThat(actual.activities).containsExactly(eighth, seventh, sixth, fifth, fourth, third, second, first)
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
