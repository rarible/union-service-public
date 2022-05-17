package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.core.elasticsearch.repository.EsActivityRepository
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.index.query.QueryBuilders.matchQuery
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import java.time.Instant

@IntegrationTest
class ActivityConsumerIT {

    @Autowired
    private lateinit var producer: RaribleKafkaProducer<ActivityDto>

    @Autowired
    private lateinit var repository: EsActivityRepository

    @Test
    fun `should consume and save activity event`(): Unit = runBlocking {
        // given
        val activity = MintActivityDto(
            id = ActivityIdDto(
                BlockchainDto.ETHEREUM,
                randomString()
            ),
            date = Instant.now(),
            blockchainInfo = ActivityBlockchainInfoDto(
                transactionHash = randomString(),
                blockHash = randomString(),
                blockNumber = randomLong(),
                logIndex = randomInt(),
            ),
            owner = UnionAddress(BlockchainGroupDto.ETHEREUM, randomString()),
            itemId = ItemIdDto(
                blockchain = BlockchainDto.ETHEREUM,
                contract = randomString(),
                tokenId = randomBigInt(),
            ),
            transactionHash = randomString(),
            value = randomBigInt(),
        )

        // when
        val message = KafkaMessage<ActivityDto>(
            key = "key",
            value = activity
        )
        producer.send(message).ensureSuccess()

        // then
        Wait.waitAssert {
            val searchQuery = NativeSearchQueryBuilder()
                .withQuery(matchQuery("activityId", activity.id.toString()))
                .build()
            val actual = repository.search(searchQuery)
            assertThat(actual.activities).isNotEmpty
            assertThat(actual.activities.first().date).isEqualTo(activity.date)
        }
    }
}
