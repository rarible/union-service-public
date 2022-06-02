package com.rarible.protocol.union.search.indexer.config

import com.ninjasquad.springmockk.SpykBean
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.test.data.randomEsActivity
import com.rarible.protocol.union.integration.flow.service.FlowItemService
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.index.query.QueryBuilders.matchQuery
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import reactor.kotlin.core.publisher.toMono
import java.time.Instant

@IntegrationTest
class ActivityConsumerIT {

    @Autowired
    private lateinit var producer: RaribleKafkaProducer<ActivityDto>

    @Autowired
    private lateinit var repository: EsActivityRepository

    @Autowired
    private lateinit var flowNftItemControllerApi: FlowNftItemControllerApi

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        repository.deleteAll()
        coEvery {
            flowNftItemControllerApi.getItemByIds(any())
        } returns FlowNftItemsDto(0, "", emptyList()).toMono() // TODO more meaningful response
    }

    @Test
    fun `should consume and save activity event`(): Unit = runBlocking {
        // given
        val activity = MintActivityDto(
            id = ActivityIdDto(
                BlockchainDto.FLOW,
                randomString()
            ),
            date = Instant.now(),
            blockchainInfo = ActivityBlockchainInfoDto(
                transactionHash = randomString(),
                blockHash = randomString(),
                blockNumber = randomLong(),
                logIndex = randomInt(),
            ),
            owner = UnionAddress(BlockchainGroupDto.FLOW, randomString()),
            itemId = ItemIdDto(
                blockchain = BlockchainDto.FLOW,
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

    @Test
    fun `should delete reverted activity`(): Unit = runBlocking {
        // given
        val id = "SOLANA:someid"
        val esActivity = randomEsActivity().copy(activityId = id)
        repository.save(esActivity)
        assertThat(repository.findById(id)).isNotNull()

        val activity = MintActivityDto(
            id = ActivityIdDto(BlockchainDto.SOLANA, "someid"),
            reverted = true,
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
            val actual = repository.findById(id)
            assertThat(actual).isNull()
        }
    }
}
