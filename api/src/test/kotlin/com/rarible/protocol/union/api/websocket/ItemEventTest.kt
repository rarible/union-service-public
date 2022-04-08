package com.rarible.protocol.union.api.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.dto.websocket.AbstractSubscribeRequest
import com.rarible.protocol.union.dto.websocket.ChangeEvent
import com.rarible.protocol.union.dto.websocket.ChangeEventType
import com.rarible.protocol.union.dto.websocket.SubscribeRequest
import com.rarible.protocol.union.dto.websocket.SubscribeRequestType
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import reactor.core.publisher.Sinks
import java.math.BigInteger
import java.time.Instant
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
internal class ItemEventTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var webSocketEventsQueue: LinkedBlockingQueue<ChangeEvent>

    @Autowired
    protected lateinit var webSocketRequests: Sinks.Many<List<AbstractSubscribeRequest>>

    @Autowired
    private lateinit var worker: ConsumerWorker<ItemEventDto>

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
        worker.start()
        val itemId = ItemIdDto(BlockchainDto.ETHEREUM, randomAddress().prefixed(), randomBigInt())

        val itemEventDto = ItemUpdateEventDto(
            itemId,
            "eventId",
            ItemDto(
                id = itemId,
                blockchain = BlockchainDto.ETHEREUM,
                lazySupply = BigInteger.ONE,
                pending = emptyList(),
                mintedAt = Instant.now(),
                lastUpdatedAt = Instant.now(),
                supply = BigInteger.ONE,
                deleted = false,
                auctions = emptyList(),
                sellers = 1
            )
        )

        webSocketRequests.tryEmitNext(
            listOf(
                SubscribeRequest(
                    type = SubscribeRequestType.ITEM,
                    id = itemId.value
                )
            )
        )

        delay(1000)
        webSocketEventsQueue.clear()


        itemProducer.send(
            KafkaMessage(
                key = itemId.value,
                value = itemEventDto
            )
        ).ensureSuccess()

        Wait.waitAssert {
            val event = webSocketEventsQueue.poll(5, TimeUnit.SECONDS)!!
            delay(2000)
            assertThat(event.type).isEqualTo(ChangeEventType.ITEM)
        }
    }
}
