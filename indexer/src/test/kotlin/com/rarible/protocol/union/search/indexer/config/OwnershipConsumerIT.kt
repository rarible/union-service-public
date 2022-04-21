package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import randomEsOwnership
import randomOwnership
import randomOwnershipId

@IntegrationTest
class OwnershipConsumerIT {

    @Autowired
    private lateinit var producer: RaribleKafkaProducer<OwnershipEventDto>

    @Autowired
    private lateinit var repository: EsOwnershipRepository

    @Test
    fun `should consume and save ownership event`(): Unit = runBlocking {
        // given
        val ownership = randomOwnership()
        val ownershipMsg = OwnershipUpdateEventDto(
            ownershipId = ownership.id,
            eventId = randomString(),
            ownership = ownership,
        )

        // when
        val message = KafkaMessage<OwnershipEventDto>(
            key = "key",
            value = ownershipMsg
        )
        producer.send(message).ensureSuccess()

        // then
        Wait.waitAssert {
            val esOwnership = repository.findById(ownership.id.fullId())
            Assertions.assertThat(esOwnership?.ownershipId).isEqualTo(ownership.id.fullId())
        }
    }

    @Test
    fun `should consume remove ownership event`() = runBlocking<Unit> {
        // given
        val ownershipId = randomOwnershipId()
        val ownershipMsg = OwnershipDeleteEventDto(
            ownershipId = ownershipId,
            eventId = randomString(),
        )
        repository.saveAll(listOf(randomEsOwnership(ownershipId)))

        // when
        val message = KafkaMessage<OwnershipEventDto>(
            key = "key",
            value = ownershipMsg
        )
        producer.send(message).ensureSuccess()

        // then
        Wait.waitAssert {
            val result = repository.findById(ownershipId.fullId())
            assertNull(result)
        }
    }
}
