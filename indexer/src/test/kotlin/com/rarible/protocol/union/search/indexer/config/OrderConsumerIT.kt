package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.core.model.EsOrder
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import com.rarible.protocol.union.search.indexer.test.orderEth
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.index.query.QueryBuilders.matchQuery
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder

@IntegrationTest
class OrderConsumerIT {

    @Autowired
    private lateinit var producer: RaribleKafkaProducer<OrderEventDto>

    @Autowired
    private lateinit var esOperations: ReactiveElasticsearchOperations

    @Test
    fun `should consume and save order event`(): Unit = runBlocking {
        // given
        val order = orderEth()
        val orderMsg = OrderUpdateEventDto(
            eventId = randomString(),
            orderId = order.id,
            order = order
        )

        // when
        val message = KafkaMessage<OrderEventDto>(
            key = "key",
            value = orderMsg
        )
        producer.send(message).ensureSuccess()

        // then
        Wait.waitAssert {
            val searchQuery = NativeSearchQueryBuilder()
                .withQuery(matchQuery("orderId", order.id.toString()))
                .build()
            val searchHits = esOperations.search(searchQuery, EsOrder::class.java).awaitFirstOrNull()
            assertThat(searchHits?.content?.orderId).isEqualTo(order.id.fullId())
        }
    }
}
