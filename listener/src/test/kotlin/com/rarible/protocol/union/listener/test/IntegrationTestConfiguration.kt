package com.rarible.protocol.union.listener.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.ext.KafkaTestExtension.Companion.kafkaContainer
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.listener.config.UnionKafkaJsonDeserializer
import com.rarible.protocol.union.listener.config.UnionKafkaJsonSerializer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class IntegrationTestConfiguration {

    @Bean
    fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
        return ApplicationEnvironmentInfo("test", "test.com")
    }

    @Bean
    fun testItemConsumer(): RaribleKafkaConsumer<UnionItemEventDto> {
        val topic = UnionEventTopicProvider.getItemTopic(applicationEnvironmentInfo().name)
        return RaribleKafkaConsumer(
            clientId = "test-union-item-consumer",
            consumerGroup = "test-union-item-group",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = UnionItemEventDto::class.java,
            defaultTopic = topic,
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun testOwnershipConsumer(): RaribleKafkaConsumer<UnionOwnershipEventDto> {
        val topic = UnionEventTopicProvider.getOwnershipTopic(applicationEnvironmentInfo().name)
        return RaribleKafkaConsumer(
            clientId = "test-union-ownership-consumer",
            consumerGroup = "test-union-ownership-group",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = UnionOwnershipEventDto::class.java,
            defaultTopic = topic,
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun testOrderConsumer(): RaribleKafkaConsumer<UnionOrderEventDto> {
        val topic = UnionEventTopicProvider.getOrderTopic(applicationEnvironmentInfo().name)
        return RaribleKafkaConsumer(
            clientId = "test-union-order-consumer",
            consumerGroup = "test-union-order-group",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = UnionOrderEventDto::class.java,
            defaultTopic = topic,
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun testActivityConsumer(): RaribleKafkaConsumer<UnionActivityDto> {
        val topic = UnionEventTopicProvider.getActivityTopic(applicationEnvironmentInfo().name)
        return RaribleKafkaConsumer(
            clientId = "test-union-activity-consumer",
            consumerGroup = "test-union-activity-group",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = UnionActivityDto::class.java,
            defaultTopic = topic,
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun testEthereumItemEventProducer(): RaribleKafkaProducer<NftItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.item",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = NftItemEventDto::class.java,
            defaultTopic = NftItemEventTopicProvider.getTopic(applicationEnvironmentInfo().name, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testEthereumOwnershipEventProducer(): RaribleKafkaProducer<NftOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ownership",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = NftOwnershipEventDto::class.java,
            defaultTopic = NftOwnershipEventTopicProvider.getTopic(applicationEnvironmentInfo().name, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testEthereumOrderEventProducer(): RaribleKafkaProducer<OrderEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.order",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = OrderEventDto::class.java,
            defaultTopic = OrderIndexerTopicProvider.getTopic(applicationEnvironmentInfo().name, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testEthereumActivityEventProducer(): RaribleKafkaProducer<ActivityDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.activity",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = ActivityDto::class.java,
            defaultTopic = ActivityTopicProvider.getTopic(applicationEnvironmentInfo().name, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

}