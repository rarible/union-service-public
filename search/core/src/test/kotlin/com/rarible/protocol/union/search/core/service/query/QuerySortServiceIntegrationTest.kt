package com.rarible.protocol.union.search.core.service.query

import com.rarible.protocol.union.search.core.ElasticActivity
import com.rarible.protocol.union.search.core.config.SearchConfiguration
import com.rarible.protocol.union.search.core.model.ActivitySort
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
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class QuerySortServiceIntegrationTest {

    @Autowired
    protected lateinit var activityEsRepository: ActivityEsRepository

    @Autowired
    private lateinit var esOperations: ReactiveElasticsearchOperations

    @Autowired
    private lateinit var service: QuerySortService

    @BeforeEach
    fun setUp() {
        activityEsRepository.deleteAll().block()
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

        activityEsRepository.saveAll(listOf(second, fourth, sixth, eighth, seventh, fifth, third, first)).awaitLast()

        // when
        service.applySort(builder, ActivitySort(latestFirst = false))
        val searchHits = esOperations.search(builder.build(), ElasticActivity::class.java).collectList().awaitFirst()
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

        activityEsRepository.saveAll(listOf(second, fourth, sixth, eighth, seventh, fifth, third, first)).awaitLast()

        // when
        service.applySort(builder, ActivitySort(latestFirst = true))
        val searchHits = esOperations.search(builder.build(), ElasticActivity::class.java).collectList().awaitFirst()
            .map { it.content }

        // then
        assertThat(searchHits).containsExactly(eighth, seventh, sixth, fifth, fourth, third, second, first)
    }

    private fun activityWithSortFields(dateMillis: Long, blockNumber: Long, logIndex: Int, salt: Long): ElasticActivity {
        return buildActivity().copy(
            date = Instant.ofEpochMilli(dateMillis),
            blockNumber = blockNumber,
            logIndex = logIndex,
            salt = salt,
        )
    }
}
