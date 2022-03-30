package com.rarible.protocol.union.search.core.repository

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.search.core.ElasticActivity
import com.rarible.protocol.union.search.core.config.SearchConfiguration
import com.rarible.protocol.union.search.test.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class ActivityEsRepositoryFt {

    @Autowired
    protected lateinit var activityEsRepository: ActivityEsRepository

    @Test
    fun `should save and read`(): Unit = runBlocking {
        val activity = ElasticActivity(
            "1234", Instant.now(), 1, 0, BlockchainDto.ETHEREUM, ActivityTypeDto.BURN,
            ElasticActivity.User(
                "0x01", null
            ),
            ElasticActivity.Collection(
                "0x02", null
            ),
            ElasticActivity.Item(
                "0x03", null
            )
        )

        val id = activityEsRepository.save(activity).awaitFirst().activityId
        val found = activityEsRepository.findById(id).awaitFirst()

        Assertions.assertThat(found).isEqualTo(activity)
    }
}