package com.rarible.protocol.union.listener.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.core.task.EnableRaribleTask
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.dto.UnionEventTopicProvider
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import com.rarible.protocol.union.listener.handler.EthOrderEventHandler
import com.rarible.protocol.order.api.subscriber.OrderIndexerEventsConsumerFactory
import com.rarible.protocol.union.dto.UnionItemEventDto
import com.rarible.protocol.union.dto.UnionOrderEventDto
import com.rarible.protocol.union.dto.UnionOwnershipEventDto
import com.rarible.protocol.union.listener.handler.EthItemEventHandler
import com.rarible.protocol.union.listener.handler.EthOwnershipEventHandler
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableRaribleTask
@EnableScaletherMongoConversions
@EnableConfigurationProperties(
    value = [
        UnionEventProducerProperties::class,
        UnionListenerProperties::class
    ]
)
class UnionListenerConfiguration(
    environmentInfo: ApplicationEnvironmentInfo,
    private val listenerProperties: UnionListenerProperties,
    private val producerProperties: UnionEventProducerProperties,
    private val meterRegistry: MeterRegistry
) {
    private val itemConsumerGroup = "${environmentInfo.name}.protocol.union.item"
    private val ownershipConsumerGroup = "${environmentInfo.name}.protocol.union.ownership"
    private val orderConsumerGroup = "${environmentInfo.name}.protocol.union.order"

    @Bean
    fun ethItemChangeWorker(
        nftIndexerEventsConsumerFactory: NftIndexerEventsConsumerFactory,
        itemEventHandler: EthItemEventHandler
    ): ConsumerWorker<NftItemEventDto> {
        return ConsumerWorker(
            consumer = nftIndexerEventsConsumerFactory.createItemEventsConsumer(itemConsumerGroup),
            properties = listenerProperties.monitoringWorker,
            eventHandler = itemEventHandler,
            meterRegistry = meterRegistry,
            workerName = "itemEventDto"
        )
    }

    @Bean
    fun ethOwnershipChangeWorker(
        nftIndexerEventsConsumerFactory: NftIndexerEventsConsumerFactory,
        ownershipEventHandler: EthOwnershipEventHandler
    ): ConsumerWorker<NftOwnershipEventDto> {
        return ConsumerWorker(
            consumer = nftIndexerEventsConsumerFactory.createOwnershipEventsConsumer(ownershipConsumerGroup),
            properties = listenerProperties.monitoringWorker,
            eventHandler = ownershipEventHandler,
            meterRegistry = meterRegistry,
            workerName = "ownershipEventDto"
        )
    }

    @Bean
    fun ethOrderChangeWorker(
        orderIndexerEventsConsumerFactory: OrderIndexerEventsConsumerFactory,
        orderEventHandler: EthOrderEventHandler
    ): ConsumerWorker<OrderEventDto> {
        return ConsumerWorker(
            consumer = orderIndexerEventsConsumerFactory.createOrderEventsConsumer(orderConsumerGroup),
            properties = listenerProperties.monitoringWorker,
            eventHandler = orderEventHandler,
            meterRegistry = meterRegistry,
            workerName = "orderEventDto"
        )
    }

    @Bean
    fun unionItemEventProducer(): RaribleKafkaProducer<UnionItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "${producerProperties.environment}.protocol-union-listener.item",
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = UnionItemEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getItemTopic(producerProperties.environment),
            bootstrapServers = producerProperties.kafkaReplicaSet
        )
    }

    @Bean
    fun unionOwnershipEventProducer(): RaribleKafkaProducer<UnionOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "${producerProperties.environment}.protocol-union-listener.ownership",
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = UnionOwnershipEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getOwnershipTopic(producerProperties.environment),
            bootstrapServers = producerProperties.kafkaReplicaSet
        )
    }

    @Bean
    fun unionOrderEventProducer(): RaribleKafkaProducer<UnionOrderEventDto> {
        return RaribleKafkaProducer(
            clientId = "${producerProperties.environment}.protocol-union-listener.order",
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = UnionOrderEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getOwnershipTopic(producerProperties.environment),
            bootstrapServers = producerProperties.kafkaReplicaSet
        )
    }

}
