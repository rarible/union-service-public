package com.rarible.protocol.union.listener.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.task.EnableRaribleTask
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.*
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.flow.nft.api.subscriber.FlowNftIndexerEventsConsumerFactory
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import com.rarible.protocol.order.api.subscriber.OrderIndexerEventsConsumerFactory
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.listener.config.activity.EthActivityEventsConsumerFactory
import com.rarible.protocol.union.listener.config.activity.FlowActivityEventsConsumerFactory
import com.rarible.protocol.union.listener.handler.SingleConsumerWorker
import com.rarible.protocol.union.listener.handler.ethereum.EthereumActivityEventHandler
import com.rarible.protocol.union.listener.handler.ethereum.EthereumItemEventHandler
import com.rarible.protocol.union.listener.handler.ethereum.EthereumOrderEventHandler
import com.rarible.protocol.union.listener.handler.ethereum.EthereumOwnershipEventHandler
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
@EnableConfigurationProperties(value = [UnionListenerProperties::class])
class UnionListenerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val properties: UnionListenerProperties,
    private val meterRegistry: MeterRegistry
) {

    companion object {
        private val FLOW = "flow"
        private val ETHEREUM = Blockchain.ETHEREUM.value
        private val POLYGON = Blockchain.POLYGON.value
    }

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host
    private val producerBrokerReplicaSet = properties.producer.brokerReplicaSet

    //------------------------ Eth Consumers ------------------------//

    @Bean
    fun ethActivityConsumerFactory(): EthActivityEventsConsumerFactory {
        val replicaSet = properties.consumer.ethereum.brokerReplicaSet
        return EthActivityEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    fun ethNftIndexerConsumerFactory(): NftIndexerEventsConsumerFactory {
        val replicaSet = properties.consumer.ethereum.brokerReplicaSet
        return NftIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    fun ethOrderIndexerConsumerFactory(): OrderIndexerEventsConsumerFactory {
        val replicaSet = properties.consumer.ethereum.brokerReplicaSet
        return OrderIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    // ------ ETHEREUM
    @Bean
    fun ethereumItemWorker(factory: NftIndexerEventsConsumerFactory): SingleConsumerWorker<NftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerGroup(Entity.ITEM), Blockchain.ETHEREUM)
        val handler = EthereumItemEventHandler(unionItemEventProducer(), Blockchain.ETHEREUM)
        return createConsumerWorker(consumer, handler, ETHEREUM, Entity.ITEM)
    }

    @Bean
    fun ethereumOwnershipWorker(factory: NftIndexerEventsConsumerFactory): SingleConsumerWorker<NftOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerGroup(Entity.OWNERSHIP), Blockchain.ETHEREUM)
        val handler = EthereumOwnershipEventHandler(unionOwnershipEventProducer(), Blockchain.ETHEREUM)
        return createConsumerWorker(consumer, handler, ETHEREUM, Entity.OWNERSHIP)
    }

    @Bean
    fun ethereumOrderWorker(factory: OrderIndexerEventsConsumerFactory): SingleConsumerWorker<OrderEventDto> {
        val consumer = factory.createOrderEventsConsumer(consumerGroup(Entity.ORDER), Blockchain.ETHEREUM)
        val handler = EthereumOrderEventHandler(unionOrderEventProducer(), Blockchain.ETHEREUM)
        return createConsumerWorker(consumer, handler, ETHEREUM, Entity.ORDER)
    }

    @Bean
    fun ethereumActivityWorker(factory: EthActivityEventsConsumerFactory): SingleConsumerWorker<ActivityDto> {
        val consumer = factory.createActivityConsumer(consumerGroup(Entity.ACTIVITY), Blockchain.ETHEREUM)
        val handler = EthereumActivityEventHandler(unionActivityEventProducer(), Blockchain.ETHEREUM)
        return createConsumerWorker(consumer, handler, ETHEREUM, Entity.ACTIVITY)
    }

    // ------ POLYGON
    @Bean
    fun polygonItemWorker(factory: NftIndexerEventsConsumerFactory): SingleConsumerWorker<NftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerGroup(Entity.ITEM), Blockchain.POLYGON)
        val handler = EthereumItemEventHandler(unionItemEventProducer(), Blockchain.POLYGON)
        return createConsumerWorker(consumer, handler, POLYGON, Entity.ITEM)
    }

    @Bean
    fun polygonOwnershipWorker(factory: NftIndexerEventsConsumerFactory): SingleConsumerWorker<NftOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerGroup(Entity.OWNERSHIP), Blockchain.POLYGON)
        val handler = EthereumOwnershipEventHandler(unionOwnershipEventProducer(), Blockchain.POLYGON)
        return createConsumerWorker(consumer, handler, POLYGON, Entity.OWNERSHIP)
    }

    @Bean
    fun polygonOrderWorker(factory: OrderIndexerEventsConsumerFactory): SingleConsumerWorker<OrderEventDto> {
        val consumer = factory.createOrderEventsConsumer(consumerGroup(Entity.ORDER), Blockchain.POLYGON)
        val handler = EthereumOrderEventHandler(unionOrderEventProducer(), Blockchain.POLYGON)
        return createConsumerWorker(consumer, handler, POLYGON, Entity.ORDER)
    }

    @Bean
    fun polygonActivityWorker(factory: EthActivityEventsConsumerFactory): SingleConsumerWorker<ActivityDto> {
        val consumer = factory.createActivityConsumer(consumerGroup(Entity.ACTIVITY), Blockchain.POLYGON)
        val handler = EthereumActivityEventHandler(unionActivityEventProducer(), Blockchain.POLYGON)
        return createConsumerWorker(consumer, handler, POLYGON, Entity.ACTIVITY)
    }

    //------------------------ Flow Consumers ------------------------//

    @Bean
    fun flowActivityConsumerFactory(): FlowActivityEventsConsumerFactory {
        val replicaSet = properties.consumer.ethereum.brokerReplicaSet
        return FlowActivityEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    fun flowNftIndexerConsumerFactory(): FlowNftIndexerEventsConsumerFactory {
        val replicaSet = properties.consumer.ethereum.brokerReplicaSet
        return FlowNftIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    fun flowItemWorker(factory: FlowNftIndexerEventsConsumerFactory): SingleConsumerWorker<FlowNftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerGroup(Entity.ITEM))
        val handler = FlowItemEventHandler(unionItemEventProducer())
        return createConsumerWorker(consumer, handler, FLOW, Entity.ITEM)
    }

    @Bean
    fun flowOwnershipWorker(factory: FlowNftIndexerEventsConsumerFactory): SingleConsumerWorker<FlowOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerGroup(Entity.OWNERSHIP))
        val handler = FlowOwnershipEventHandler(unionOwnershipEventProducer())
        return createConsumerWorker(consumer, handler, FLOW, Entity.OWNERSHIP)
    }

    @Bean
    fun flowOrderChangeWorker(factory: FlowNftIndexerEventsConsumerFactory): SingleConsumerWorker<FlowOrderEventDto> {
        val handler = FlowOrderEventHandler(unionOrderEventProducer())
        val consumer = factory.createORderEventsConsumer(consumerGroup(Entity.ORDER))
        return createConsumerWorker(consumer, handler, FLOW, Entity.ORDER)
    }

    @Bean
    fun flowActivityWorker(factory: FlowActivityEventsConsumerFactory): SingleConsumerWorker<FlowActivityDto> {
        val handler = FlowActivityEventHandler(unionActivityEventProducer())
        val consumer = factory.createActivityConsumer(consumerGroup(Entity.ACTIVITY))
        return createConsumerWorker(consumer, handler, FLOW, Entity.ACTIVITY)
    }

    private fun <T> createConsumerWorker(
        consumer: RaribleKafkaConsumer<T>,
        handler: ConsumerEventHandler<T>,
        blockchain: String,
        entityType: String
    ): SingleConsumerWorker<T> {
        return SingleConsumerWorker(
            ConsumerWorker(
                consumer = consumer,
                properties = properties.monitoringWorker,
                eventHandler = handler,
                meterRegistry = meterRegistry,
                workerName = "${blockchain}-${entityType}"
            )
        )
    }

    private fun consumerGroup(suffix: String): String {
        return "${env}.protocol.union.${suffix}"
    }

    //------------------------ Union Producers ------------------------//

    @Bean
    fun unionItemEventProducer(): RaribleKafkaProducer<UnionItemEventDto> {
        val unionItemTopic = UnionEventTopicProvider.getItemTopic(env)
        return createUnionProducer(Entity.ITEM, unionItemTopic, UnionItemEventDto::class.java)
    }

    @Bean
    fun unionOwnershipEventProducer(): RaribleKafkaProducer<UnionOwnershipEventDto> {
        val unionOwnershipTopic = UnionEventTopicProvider.getOwnershipTopic(env)
        return createUnionProducer(Entity.OWNERSHIP, unionOwnershipTopic, UnionOwnershipEventDto::class.java)
    }

    @Bean
    fun unionOrderEventProducer(): RaribleKafkaProducer<UnionOrderEventDto> {
        val unionOrderTopic = UnionEventTopicProvider.getOrderTopic(env)
        return createUnionProducer(Entity.ORDER, unionOrderTopic, UnionOrderEventDto::class.java)
    }

    @Bean
    fun unionActivityEventProducer(): RaribleKafkaProducer<UnionActivityDto> {
        val unionActivityTopic = UnionEventTopicProvider.getActivityTopic(env)
        return createUnionProducer(Entity.ACTIVITY, unionActivityTopic, UnionActivityDto::class.java)
    }

    private fun <T> createUnionProducer(clientSuffix: String, topic: String, type: Class<T>): RaribleKafkaProducer<T> {
        return RaribleKafkaProducer(
            clientId = "${env}.protocol-union-listener.${clientSuffix}",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = type,
            defaultTopic = topic,
            bootstrapServers = producerBrokerReplicaSet
        )
    }
}
