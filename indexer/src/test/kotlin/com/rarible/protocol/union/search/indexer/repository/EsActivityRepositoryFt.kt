package com.rarible.protocol.union.search.indexer.repository

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.SearchConfiguration
import com.rarible.protocol.union.core.elasticsearch.repository.EsActivityRepository
import com.rarible.protocol.union.core.test.randomEsActivity
import com.rarible.protocol.union.core.test.randomInstant
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.index.query.BoolQueryBuilder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.test.context.ContextConfiguration
import kotlin.random.Random.Default.nextLong

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class EsActivityRepositoryFt {

    @Autowired
    protected lateinit var repository: EsActivityRepository

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
}
