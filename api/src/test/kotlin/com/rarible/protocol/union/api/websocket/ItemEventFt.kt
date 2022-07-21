package com.rarible.protocol.union.api.websocket

import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.test.WaitAssert
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemSubscriptionEventDto
import com.rarible.protocol.union.dto.ItemSubscriptionRequestDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.dto.SubscriptionActionDto
import com.rarible.protocol.union.dto.SubscriptionEventDto
import com.rarible.protocol.union.dto.SubscriptionRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import reactor.core.publisher.Sinks
import java.math.BigInteger
import java.util.Queue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "application.environment = test",
        "spring.cloud.consul.config.enabled = false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "local.server.port = 9090",
        "local.server.host = localhost"
    ]
)

@IntegrationTest
@ContextConfiguration
internal class ItemEventFt : AbstractIntegrationTest() {

    @Autowired
    protected lateinit var webSocketEventsQueue: LinkedBlockingQueue<SubscriptionEventDto>

    @Autowired
    protected lateinit var webSocketRequests: Sinks.Many<List<SubscriptionRequestDto>>

    private fun <T> filterByValueType(messages: Queue<KafkaMessage<Any>>, type: Class<T>): Collection<KafkaMessage<T>> {
        return messages.filter {
            type.isInstance(it.value)
        }.map {
            it as KafkaMessage<T>
        }
    }

    fun findItemUpdates(itemId: String): List<KafkaMessage<ItemUpdateEventDto>> {
        return filterByValueType(itemEvents as Queue<KafkaMessage<Any>>, ItemUpdateEventDto::class.java)
            .filter { it.value.itemId.value == itemId }
    }

    @Test
    fun `item event websocket test`() = runWithKafka {
        val itemId = ItemIdDto(BlockchainDto.ETHEREUM, randomAddress().prefixed(), randomBigInt())

        val itemEventDto = ItemUpdateEventDto(
            itemId,
            "eventId",
            ItemDto(
                id = itemId,
                blockchain = BlockchainDto.ETHEREUM,
                lazySupply = BigInteger.ONE,
                pending = emptyList(),
                mintedAt = nowMillis(),
                lastUpdatedAt = nowMillis(),
                supply = BigInteger.ONE,
                deleted = false,
                auctions = emptyList(),
                sellers = 1
            )
        )

        webSocketRequests.tryEmitNext(listOf(ItemSubscriptionRequestDto(SubscriptionActionDto.SUBSCRIBE, itemId)))

        delay(1000)
        webSocketEventsQueue.clear()

        itemProducer.send(
            KafkaMessage(
                key = itemId.value,
                value = itemEventDto
            )
        ).ensureSuccess()

        WaitAssert.wait {
            val event = withContext(Dispatchers.IO) {
                webSocketEventsQueue.poll(5, TimeUnit.SECONDS)
            }
            assertThat(event).isNotNull
            assertThat(event).isInstanceOf(ItemSubscriptionEventDto::class.java)
        }
    }
}
