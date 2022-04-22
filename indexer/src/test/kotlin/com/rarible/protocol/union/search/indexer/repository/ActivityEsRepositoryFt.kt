package com.rarible.protocol.union.search.indexer.repository

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.test.context.ContextConfiguration
import randomInstant
import java.time.Instant
import kotlin.random.Random.Default.nextLong

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class ActivityEsRepositoryFt {

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
        Assertions.assertThat(found).isEqualTo(activity)
    }
}