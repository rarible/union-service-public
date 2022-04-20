package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.elasticsearch.index.query.QueryBuilders
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import randomOwnership

@IntegrationTest
class OwnershipConsumerIT {

    @Autowired
    private lateinit var producer: RaribleKafkaProducer<OwnershipEventDto>

    @Autowired
    private lateinit var esOperations: ReactiveElasticsearchOperations

    @Test
    fun `should consume and save ownership event`(): Unit = runBlocking {
        // given
        val ownership = randomOwnership()
        val ownershipMsg = OwnershipUpdateEventDto(
            ownershipId = ownership.id,
            eventId = randomString(),
            ownership = ownership
        )

        // when
        val message = KafkaMessage<OwnershipEventDto>(
            key = "key",
            value = ownershipMsg
        )
        producer.send(message).ensureSuccess()

        // then
        Wait.waitAssert {
            val searchQuery = NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchQuery("ownershipId", ownership.id.fullId()))
                .build()
            val searchHits = esOperations.search(searchQuery, EsOwnership::class.java).awaitFirstOrNull()
            Assertions.assertThat(searchHits?.content?.ownershipId).isEqualTo(ownership.id.fullId())
        }
    }
}
