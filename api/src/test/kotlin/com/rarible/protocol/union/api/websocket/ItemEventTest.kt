package com.rarible.protocol.union.api.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.api.configuration.WebSocketConfiguration
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.union.TestCommonConfiguration.Companion.kafkaContainer
import com.rarible.protocol.union.api.configuration.ApiConfiguration
import com.rarible.protocol.union.api.configuration.UnionListenerConfig
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.api.dto.*
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.listener.config.UnionListenerConfiguration
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import reactor.core.publisher.Sinks
import java.math.BigInteger
import java.time.Instant
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@ContextConfiguration(
    classes = [
        UnionListenerConfig::class,
        WebSocketConfiguration::class
    ]
)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = [
        "application.environment = test",
        "spring.cloud.consul.config.enabled = false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@IntegrationTest
internal class ItemEventTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var webSocketEventsQueue: LinkedBlockingQueue<ChangeEvent>

    @Autowired
    protected lateinit var webSocketRequests: Sinks.Many<List<AbstractSubscribeRequest>>


    @Test
    fun `item event websocket test`() {
        val itemId = ItemIdDto(BlockchainDto.ETHEREUM, randomAddress().prefixed(), randomBigInt())

        webSocketRequests.tryEmitNext(
            listOf(
                SubscribeRequest(
                    type = SubscribeRequestType.ITEM,
                    id = itemId.fullId()
                )
            )
        )

        val raribleKafkaProducer = RaribleKafkaProducer(
            clientId = "test-client",
            defaultTopic = "protocol.e2e.union.activity",
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = ItemDto::class.java
        )
        val kafkaMessage = KafkaMessage(
            key = "event_id",
            value = ItemDto(
                id = ItemIdDto(BlockchainDto.ETHEREUM, randomAddress().prefixed(), randomBigInt()),
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
        runBlocking {
            raribleKafkaProducer.send(kafkaMessage)
        }

        val event = webSocketEventsQueue.poll(5, TimeUnit.SECONDS)!!
        assertThat(event.type).isEqualTo(ChangeEventType.ITEM)
        val item = objectMapper.convertValue(event.value, ItemDto::class.java)
        assertThat(item.tokenId).isEqualTo(itemId)
    }
}
