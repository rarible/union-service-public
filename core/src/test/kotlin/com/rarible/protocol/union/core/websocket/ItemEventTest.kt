package com.rarible.protocol.union.core.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.marketplace.core.test.Factories.ADDRESS_ONE
import com.rarible.marketplace.core.test.Factories.ADDRESS_TWO
import com.rarible.protocol.union.TestCommonConfiguration.Companion.kafkaContainer
import com.rarible.protocol.union.core.domain.Item
import com.rarible.protocol.union.core.event.*
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Sinks
import java.math.BigInteger
import java.time.Instant
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

internal class ItemEventTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var webSocketEventsQueue: LinkedBlockingQueue<ChangeEvent>

    @Autowired
    protected lateinit var webSocketRequests: Sinks.Many<List<AbstractSubscribeRequest>>


    @Test
    suspend fun `item event websocket test`() {
        val token = ADDRESS_ONE
        val tokenId = "0"
        val ownershipId = ADDRESS_TWO

        val itemId = Item.getId(token, tokenId)
        webSocketRequests.tryEmitNext(
            listOf(
                SubscribeRequest(
                    type = SubscribeRequestType.ITEM,
                    id = itemId
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
            key = itemId,
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
            ) as ItemDto
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
