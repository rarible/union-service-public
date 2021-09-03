package com.rarible.protocol.union.listener.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.task.EnableRaribleTask
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.protocol.dto.*
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.flow.nft.api.subscriber.FlowNftIndexerEventsConsumerFactory
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import com.rarible.protocol.order.api.subscriber.OrderIndexerEventsConsumerFactory
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.listener.config.activity.EthActivityEventsConsumerFactory
import com.rarible.protocol.union.listener.config.activity.EthActivityEventsSubscriberProperties
import com.rarible.protocol.union.listener.config.activity.FlowActivityEventsConsumerFactory
import com.rarible.protocol.union.listener.config.activity.FlowActivityEventsSubscriberProperties
import com.rarible.protocol.union.listener.handler.SingleKafkaConsumerWorker
import com.rarible.protocol.union.listener.handler.ethereum.EthereumCompositeConsumerWorker
import com.rarible.protocol.union.listener.handler.ethereum.EthereumCompositeConsumerWorker.ConsumerEventHandlerFactory
import com.rarible.protocol.union.listener.handler.ethereum.EthereumCompositeConsumerWorker.ConsumerFactory
import com.rarible.protocol.union.listener.handler.ethereum.EthereumEventHandlerFactory
import com.rarible.protocol.union.listener.handler.flow.FlowActivityEventHandler
import com.rarible.protocol.union.listener.handler.flow.FlowItemEventHandler
import com.rarible.protocol.union.listener.handler.flow.FlowOrderEventHandler
import com.rarible.protocol.union.listener.handler.flow.FlowOwnershipEventHandler
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
        UnionListenerProperties::class,
        EthActivityEventsSubscriberProperties::class,
        FlowActivityEventsSubscriberProperties::class
    ]
)
class UnionListenerConfiguration(
    private val environmentInfo: ApplicationEnvironmentInfo,
    private val listenerProperties: UnionListenerProperties,
    private val producerProperties: UnionEventProducerProperties,
    private val ethActivitySubscriberProperties: EthActivityEventsSubscriberProperties,
    private val flowActivitySubscriberProperties: FlowActivityEventsSubscriberProperties,
    private val meterRegistry: MeterRegistry
) {
    private val itemConsumerGroup = "${environmentInfo.name}.protocol.union.item"
    private val ownershipConsumerGroup = "${environmentInfo.name}.protocol.union.ownership"
    private val orderConsumerGroup = "${environmentInfo.name}.protocol.union.order"
    private val activityConsumerGroup = "${environmentInfo.name}.protocol.union.activity"


    //------------------------ Eth Consumers ------------------------//

    @Bean
    fun ethActivityConsumerFactory(): EthActivityEventsConsumerFactory {
        return EthActivityEventsConsumerFactory(
            brokerReplicaSet = ethActivitySubscriberProperties.brokerReplicaSet,
            host = environmentInfo.host,
            environment = environmentInfo.name
        )
    }

    @Bean
    fun ethereumItemChangeWorker(
        nftIndexerEventsConsumerFactory: NftIndexerEventsConsumerFactory,
        eventHandlerFactory: EthereumEventHandlerFactory
    ): EthereumCompositeConsumerWorker<NftItemEventDto> {
        return EthereumCompositeConsumerWorker(
            consumerFactory = ConsumerFactory.wrap { group, blockchain ->
                nftIndexerEventsConsumerFactory.createItemEventsConsumer(group, blockchain)
            },
            eventHandlerFactory = ConsumerEventHandlerFactory.wrap { blockchain ->
                eventHandlerFactory.createItemEventHandler(blockchain)
            },
            consumerGroup = itemConsumerGroup,
            properties = listenerProperties.monitoringWorker,
            meterRegistry = meterRegistry,
            workerName = "ethItemEventDto"
        )
    }

    @Bean
    fun ethereumOwnershipChangeWorker(
        nftIndexerEventsConsumerFactory: NftIndexerEventsConsumerFactory,
        eventHandlerFactory: EthereumEventHandlerFactory
    ): EthereumCompositeConsumerWorker<NftOwnershipEventDto> {
        return EthereumCompositeConsumerWorker(
            consumerFactory = ConsumerFactory.wrap { group, blockchain ->
                nftIndexerEventsConsumerFactory.createOwnershipEventsConsumer(group, blockchain)
            },
            eventHandlerFactory = ConsumerEventHandlerFactory.wrap { blockchain ->
                eventHandlerFactory.createOwnershipEventHandler(blockchain)
            },
            consumerGroup = ownershipConsumerGroup,
            properties = listenerProperties.monitoringWorker,
            meterRegistry = meterRegistry,
            workerName = "ethOwnershipEventDto"
        )
    }

    @Bean
    fun ethereumOrderChangeWorker(
        orderIndexerEventsConsumerFactory: OrderIndexerEventsConsumerFactory,
        eventHandlerFactory: EthereumEventHandlerFactory
    ): EthereumCompositeConsumerWorker<OrderEventDto> {
        return EthereumCompositeConsumerWorker(
            consumerFactory = ConsumerFactory.wrap { group, blockchain ->
                orderIndexerEventsConsumerFactory.createOrderEventsConsumer(group, blockchain)
            },
            eventHandlerFactory = ConsumerEventHandlerFactory.wrap { blockchain ->
                eventHandlerFactory.createOrderEventHandler(blockchain)
            },
            consumerGroup = orderConsumerGroup,
            properties = listenerProperties.monitoringWorker,
            meterRegistry = meterRegistry,
            workerName = "ethOrderEventDto"
        )
    }

    @Bean
    fun ethereumActivityWorker(
        eventHandlerFactory: EthereumEventHandlerFactory
    ): EthereumCompositeConsumerWorker<ActivityDto> {
        return EthereumCompositeConsumerWorker(
            consumerFactory = ConsumerFactory.wrap { group, blockchain ->
                ethActivityConsumerFactory().createActivityConsumer(group, blockchain)
            },
            eventHandlerFactory = ConsumerEventHandlerFactory.wrap { blockchain ->
                eventHandlerFactory.createActivityEventHandler(blockchain)
            },
            consumerGroup = activityConsumerGroup,
            properties = listenerProperties.monitoringWorker,
            meterRegistry = meterRegistry,
            workerName = "ethActivityEventDto"
        )
    }

    //------------------------ Flow Consumers ------------------------//

    @Bean
    fun flowActivityConsumerFactory(): FlowActivityEventsConsumerFactory {
        return FlowActivityEventsConsumerFactory(
            brokerReplicaSet = flowActivitySubscriberProperties.brokerReplicaSet,
            host = environmentInfo.host,
            environment = environmentInfo.name
        )
    }

    @Bean
    fun flowItemChangeWorker(
        flowNftIndexerEventsConsumerFactory: FlowNftIndexerEventsConsumerFactory,
        flowItemEventHandler: FlowItemEventHandler
    ): SingleKafkaConsumerWorker<FlowNftItemEventDto> {
        return SingleKafkaConsumerWorker(
            ConsumerWorker(
                consumer = flowNftIndexerEventsConsumerFactory.createItemEventsConsumer(itemConsumerGroup),
                properties = listenerProperties.monitoringWorker,
                eventHandler = flowItemEventHandler,
                meterRegistry = meterRegistry,
                workerName = "flowItemEventDto"
            )
        )
    }

    @Bean
    fun flowOwnershipChangeWorker(
        flowNftIndexerEventsConsumerFactory: FlowNftIndexerEventsConsumerFactory,
        flowOwnershipEventHandler: FlowOwnershipEventHandler
    ): SingleKafkaConsumerWorker<FlowOwnershipEventDto> {
        return SingleKafkaConsumerWorker(
            ConsumerWorker(
                consumer = flowNftIndexerEventsConsumerFactory.createOwnershipEventsConsumer(ownershipConsumerGroup),
                properties = listenerProperties.monitoringWorker,
                eventHandler = flowOwnershipEventHandler,
                meterRegistry = meterRegistry,
                workerName = "flowOwnershipEventDto"
            )
        )
    }


    @Bean
    fun flowOrderChangeWorker(
        flowNftIndexerEventsConsumerFactory: FlowNftIndexerEventsConsumerFactory,
        flowOrderEventHandler: FlowOrderEventHandler
    ): SingleKafkaConsumerWorker<FlowOrderEventDto> {
        return SingleKafkaConsumerWorker(
            ConsumerWorker(
                consumer = flowNftIndexerEventsConsumerFactory.createORderEventsConsumer(orderConsumerGroup),
                properties = listenerProperties.monitoringWorker,
                eventHandler = flowOrderEventHandler,
                meterRegistry = meterRegistry,
                workerName = "flowOrderEventDto"
            )
        )
    }

    @Bean
    fun flowActivityWorker(
        flowActivityConsumerFactory: FlowActivityEventsConsumerFactory,
        flowActivityEventHandler: FlowActivityEventHandler
    ): SingleKafkaConsumerWorker<FlowActivityDto> {
        return SingleKafkaConsumerWorker(
            ConsumerWorker(
                consumer = flowActivityConsumerFactory.createActivityConsumer(orderConsumerGroup),
                properties = listenerProperties.monitoringWorker,
                eventHandler = flowActivityEventHandler,
                meterRegistry = meterRegistry,
                workerName = "flowActivityEventDto"
            )
        )
    }

    //------------------------ Union Producers ------------------------//

    @Bean
    fun unionItemEventProducer(): RaribleKafkaProducer<UnionItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "${producerProperties.environment}.protocol-union-listener.item",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = UnionItemEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getItemTopic(producerProperties.environment),
            bootstrapServers = producerProperties.kafkaReplicaSet
        )
    }

    @Bean
    fun unionOwnershipEventProducer(): RaribleKafkaProducer<UnionOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "${producerProperties.environment}.protocol-union-listener.ownership",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = UnionOwnershipEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getOwnershipTopic(producerProperties.environment),
            bootstrapServers = producerProperties.kafkaReplicaSet
        )
    }

    @Bean
    fun unionOrderEventProducer(): RaribleKafkaProducer<UnionOrderEventDto> {
        return RaribleKafkaProducer(
            clientId = "${producerProperties.environment}.protocol-union-listener.order",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = UnionOrderEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getOrderTopic(producerProperties.environment),
            bootstrapServers = producerProperties.kafkaReplicaSet
        )
    }

    @Bean
    fun unionActivityEventProducer(): RaribleKafkaProducer<UnionActivityDto> {
        return RaribleKafkaProducer(
            clientId = "${producerProperties.environment}.protocol-union-listener.activity",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = UnionActivityDto::class.java,
            defaultTopic = UnionEventTopicProvider.getActivityTopic(producerProperties.environment),
            bootstrapServers = producerProperties.kafkaReplicaSet
        )
    }
}
