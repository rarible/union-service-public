package com.rarible.protocol.union.listener.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.ext.KafkaTestExtension.Companion.kafkaContainer
import com.rarible.protocol.dto.*
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.listener.config.UnionKafkaJsonSerializer
import com.rarible.protocol.union.listener.config.activity.FlowActivityTopicProvider
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@TestConfiguration
@Import(CoreConfiguration::class)
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
            clientId = "test.union.ethereum.item",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = NftItemEventDto::class.java,
            defaultTopic = NftItemEventTopicProvider.getTopic(applicationEnvironmentInfo().name, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testEthereumOwnershipEventProducer(): RaribleKafkaProducer<NftOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.ownership",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = NftOwnershipEventDto::class.java,
            defaultTopic = NftOwnershipEventTopicProvider.getTopic(applicationEnvironmentInfo().name, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testEthereumOrderEventProducer(): RaribleKafkaProducer<OrderEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.order",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = OrderEventDto::class.java,
            defaultTopic = OrderIndexerTopicProvider.getUpdateTopic(applicationEnvironmentInfo().name, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testEthereumActivityEventProducer(): RaribleKafkaProducer<ActivityDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.activity",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = ActivityDto::class.java,
            defaultTopic = ActivityTopicProvider.getTopic(applicationEnvironmentInfo().name, "ethereum"),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testFlowItemEventProducer(): RaribleKafkaProducer<FlowNftItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.flow.item",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = FlowNftItemEventDto::class.java,
            defaultTopic = FlowNftItemEventTopicProvider.getTopic(applicationEnvironmentInfo().name),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testFlowOwnershipEventProducer(): RaribleKafkaProducer<FlowOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.flow.ownership",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = FlowOwnershipEventDto::class.java,
            defaultTopic = FlowNftOwnershipEventTopicProvider.getTopic(applicationEnvironmentInfo().name),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testFlowOrderEventProducer(): RaribleKafkaProducer<FlowOrderEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.flow.order",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = FlowOrderEventDto::class.java,
            defaultTopic = FlowOrderEventTopicProvider.getTopic(applicationEnvironmentInfo().name),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testFlowActivityEventProducer(): RaribleKafkaProducer<FlowActivityDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.flow.activity",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = FlowActivityDto::class.java,
            defaultTopic = FlowActivityTopicProvider.getTopic(applicationEnvironmentInfo().name),
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
    }

}