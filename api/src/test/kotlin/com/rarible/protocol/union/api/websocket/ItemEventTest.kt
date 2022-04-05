package com.rarible.protocol.union.api.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.api.dto.AbstractSubscribeRequest
import com.rarible.protocol.union.api.dto.ChangeEvent
import com.rarible.protocol.union.api.dto.SubscribeRequest
import com.rarible.protocol.union.api.dto.SubscribeRequestType
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import reactor.core.publisher.Sinks
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
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
@ContextConfiguration
@IntegrationTest
internal class ItemEventTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var webSocketEventsQueue: LinkedBlockingQueue<ChangeEvent>

    @Autowired
    protected lateinit var webSocketRequests: Sinks.Many<List<AbstractSubscribeRequest>>

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

        webSocketRequests.tryEmitNext(
            listOf(
                SubscribeRequest(
                    type = SubscribeRequestType.ITEM,
                    id = itemId.value
                )
            )
        )

            //val itemId = randomEthItemId()
            val ethItem = randomEthNftItemDto(itemId)

            ethItemProducer.send(
                KafkaMessage(
                    key = itemId.value,
                    value = NftItemUpdateEventDto(
                        eventId = randomString(),
                        itemId = itemId.value,
                        item = ethItem
                    )
                )
            ).ensureSuccess()

            Wait.waitAssert {
                val messages = findItemUpdates(itemId.value)
                Assertions.assertThat(messages).hasSize(1)
                Assertions.assertThat(messages[0].key).isEqualTo(itemId.fullId())
                Assertions.assertThat(messages[0].id).isEqualTo(itemId.fullId())
                Assertions.assertThat(messages[0].value.itemId).isEqualTo(itemId)
            }

          /*  val event = webSocketEventsQueue.poll(5, TimeUnit.SECONDS)!!
            assertThat(event.type).isEqualTo(ChangeEventType.ITEM)
            val item = objectMapper.convertValue(event.value, ItemDto::class.java)
            assertThat(item.tokenId).isEqualTo(itemId)*/
    }
}
