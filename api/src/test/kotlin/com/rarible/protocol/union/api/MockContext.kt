package com.rarible.protocol.union.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.ext.KafkaTestExtension
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemEventTopicProvider
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipEventTopicProvider
import com.rarible.protocol.union.api.dto.AbstractSubscribeRequest
import com.rarible.protocol.union.api.dto.ChangeEvent
import com.rarible.protocol.union.api.dto.ChangeEventType
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Sinks
import java.net.URI
import java.util.concurrent.LinkedBlockingQueue

@Configuration
@EnableAutoConfiguration
class MockContext : ApplicationListener<WebServerInitializedEvent> {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Bean
    fun shutdownMono() = Sinks.one<Void>()

    @Bean
    fun webSocketEventsQueue() = LinkedBlockingQueue<ChangeEvent>()

    @Bean
    fun webSocketRequests(): Sinks.Many<List<AbstractSubscribeRequest>> = Sinks.many().unicast()
        .onBackpressureBuffer()

   /* @Bean
    fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
        return ApplicationEnvironmentInfo("test", "test.com")
    }
*/
    @Bean
    fun testItemConsumer(): RaribleKafkaConsumer<ItemEventDto> {
        val topic = UnionEventTopicProvider.getItemTopic(/*applicationEnvironmentInfo().name*/"test")
        return RaribleKafkaConsumer(
            clientId = "test-union-item-consumer",
            consumerGroup = "test-union-item-group",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = ItemEventDto::class.java,
            defaultTopic = topic,
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun testOwnershipConsumer(): RaribleKafkaConsumer<OwnershipEventDto> {
        val topic = UnionEventTopicProvider.getOwnershipTopic(/*applicationEnvironmentInfo().name*/"test")
        return RaribleKafkaConsumer(
            clientId = "test-union-ownership-consumer",
            consumerGroup = "test-union-ownership-group",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = OwnershipEventDto::class.java,
            defaultTopic = topic,
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun testEthereumItemEventProducer(): RaribleKafkaProducer<NftItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.item",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = NftItemEventDto::class.java,
            defaultTopic = NftItemEventTopicProvider.getTopic(/*applicationEnvironmentInfo().name*/"test", "ethereum"),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testEthereumOwnershipEventProducer(): RaribleKafkaProducer<NftOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.ownership",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = NftOwnershipEventDto::class.java,
            defaultTopic = NftOwnershipEventTopicProvider.getTopic(/*applicationEnvironmentInfo().name*/"test", "ethereum"),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers()
        )
    }

    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        val port = event.webServer.port

        ReactorNettyWebSocketClient().execute(URI("ws://127.0.0.1:$port$/subscribe")) { session ->
            session.receive().map { notification ->
                val event = objectMapper.readValue(
                    notification.payloadAsText,
                    ChangeEvent::class.java
                )
                if (event.type != ChangeEventType.FAKE) {
                    webSocketEventsQueue().offer(event)
                }
            }.subscribe()
            session.send(webSocketRequests().asFlux().map {
                WebSocketMessage(
                    WebSocketMessage.Type.TEXT,
                    DefaultDataBufferFactory().wrap(
                        objectMapper.writeValueAsBytes(
                            it
                        )
                    )
                )
            }).subscribe()
            shutdownMono().asMono()
        }.subscribe()
    }
}
