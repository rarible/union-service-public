package com.rarible.protocol.union.search.indexer.repository

import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.test.data.randomEsActivity
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.index.query.BoolQueryBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.test.context.ContextConfiguration
import randomInstant
import kotlin.random.Random.Default.nextLong

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class EsActivityRepositoryFt {

    @Autowired
    protected lateinit var repository: EsActivityRepository

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        elasticsearchTestBootstrapper.bootstrap()
    }

    @Test
    fun `should save and read`(): Unit = runBlocking {
        val activity = EsActivity(
            "1234", randomInstant(), 1, 0, salt = nextLong(), BlockchainDto.ETHEREUM, ActivityTypeDto.BURN,
            "0x01",
            null,
            "0x02",
            "0x03",
        )

        val id = repository.save(activity).activityId
        val found = repository.findById(id)
        assertThat(found).isEqualTo(activity)
    }

    @Test
    fun `should be able to search up to 1000 activities`(): Unit = runBlocking {
        // given
        val activities = List(1000) { randomEsActivity() }
        repository.saveAll(activities)

        // when
        val query = NativeSearchQuery(BoolQueryBuilder())
        query.maxResults = 1000
        val actual = repository.search(query)

        // then
        assertThat(actual.activities).hasSize(1000)
    }

    @Test
    fun `should delete activities by ids`(): Unit = runBlocking {
        // given
        val activities = List(30) { randomEsActivity() }
        repository.saveAll(activities)
        val ids = activities.map { it.activityId }

        // when
        val deleted = repository.delete(ids)
        val query = NativeSearchQuery(BoolQueryBuilder())
        query.maxResults = 100
        val actual = repository.search(query)

        // then
        assertThat(deleted).isEqualTo(30)
        assertThat(actual.activities).hasSize(0)
    }
}
