package com.rarible.protocol.union.listener.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.flow.nft.api.subscriber.FlowNftIndexerEventsConsumerFactory
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import com.rarible.protocol.order.api.subscriber.OrderIndexerEventsConsumerFactory
import com.rarible.protocol.union.core.Entity
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.enrichment.configuration.EnrichmentConfiguration
import com.rarible.protocol.union.enrichment.service.event.EnrichmentItemEventService
import com.rarible.protocol.union.enrichment.service.event.EnrichmentOrderEventService
import com.rarible.protocol.union.enrichment.service.event.EnrichmentOwnershipEventService
import com.rarible.protocol.union.listener.config.activity.EthActivityEventsConsumerFactory
import com.rarible.protocol.union.listener.config.activity.FlowActivityEventsConsumerFactory
import com.rarible.protocol.union.listener.handler.BatchedConsumerWorker
import com.rarible.protocol.union.listener.handler.KafkaConsumerWorker
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
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(EnrichmentConfiguration::class)
@EnableConfigurationProperties(value = [UnionListenerProperties::class])
class UnionListenerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val properties: UnionListenerProperties,
    private val meterRegistry: MeterRegistry,

    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService,
    private val enrichmentOrderEventService: EnrichmentOrderEventService,

    private val itemProducer: RaribleKafkaProducer<ItemEventDto>,
    private val ownershipProducer: RaribleKafkaProducer<OwnershipEventDto>,
    private val orderProducer: RaribleKafkaProducer<OrderEventDto>,
    private val activityProducer: RaribleKafkaProducer<ActivityDto>
) {

    companion object {
        private val FLOW = BlockchainDto.FLOW.name.toLowerCase()
        private val ETHEREUM = BlockchainDto.ETHEREUM.name.toLowerCase()
        private val POLYGON = BlockchainDto.POLYGON.name.toLowerCase()
    }

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val ethereumProperties = properties.consumer.ethereum
    private val polygonProperties = properties.consumer.polygon
    private val flowProperties = properties.consumer.flow

    //------------------------ Eth Consumers ------------------------//

    // ------ ETHEREUM
    @Bean
    @Qualifier("ethereum.nft.consumer.factory")
    fun ethNftIndexerConsumerFactory(): NftIndexerEventsConsumerFactory {
        val replicaSet = ethereumProperties.brokerReplicaSet
        return NftIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    @Qualifier("ethereum.order.consumer.factory")
    fun ethOrderIndexerConsumerFactory(): OrderIndexerEventsConsumerFactory {
        val replicaSet = ethereumProperties.brokerReplicaSet
        return OrderIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    @Qualifier("ethereum.activity.consumer.factory")
    fun ethActivityConsumerFactory(): EthActivityEventsConsumerFactory {
        val replicaSet = ethereumProperties.brokerReplicaSet
        return EthActivityEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    fun ethereumItemWorker(@Qualifier("ethereum.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory): KafkaConsumerWorker<NftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerGroup(Entity.ITEM), Blockchain.ETHEREUM)
        val handler = EthereumItemEventHandler(enrichmentItemEventService, BlockchainDto.ETHEREUM)
        return createBatchedConsumerWorker(consumer, handler, ETHEREUM, Entity.ITEM, ethereumProperties.itemWorkers)
    }

    @Bean
    fun ethereumOwnershipWorker(@Qualifier("ethereum.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory): KafkaConsumerWorker<NftOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerGroup(Entity.OWNERSHIP), Blockchain.ETHEREUM)
        val handler = EthereumOwnershipEventHandler(enrichmentOwnershipEventService, BlockchainDto.ETHEREUM)
        return createBatchedConsumerWorker(
            consumer,
            handler,
            ETHEREUM,
            Entity.OWNERSHIP,
            ethereumProperties.ownershipWorkers
        )
    }

    @Bean
    fun ethereumOrderWorker(@Qualifier("ethereum.order.consumer.factory") factory: OrderIndexerEventsConsumerFactory): KafkaConsumerWorker<com.rarible.protocol.dto.OrderEventDto> {
        val consumer = factory.createOrderEventsConsumer(consumerGroup(Entity.ORDER), Blockchain.ETHEREUM)
        val handler = EthereumOrderEventHandler(orderProducer, enrichmentOrderEventService, BlockchainDto.ETHEREUM)
        return createBatchedConsumerWorker(consumer, handler, ETHEREUM, Entity.ORDER, ethereumProperties.orderWorkers)
    }

    @Bean
    fun ethereumActivityWorker(@Qualifier("ethereum.activity.consumer.factory") factory: EthActivityEventsConsumerFactory): KafkaConsumerWorker<com.rarible.protocol.dto.ActivityDto> {
        val consumer = factory.createActivityConsumer(consumerGroup(Entity.ACTIVITY), Blockchain.ETHEREUM)
        val handler = EthereumActivityEventHandler(activityProducer, BlockchainDto.ETHEREUM)
        return createSingleConsumerWorker(consumer, handler, ETHEREUM, Entity.ACTIVITY)
    }

    // ------ POLYGON
    @Bean
    @Qualifier("polygon.nft.consumer.factory")
    fun polygonNftIndexerConsumerFactory(): NftIndexerEventsConsumerFactory {
        val replicaSet = polygonProperties.brokerReplicaSet
        return NftIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    @Qualifier("polygon.order.consumer.factory")
    fun polygonOrderIndexerConsumerFactory(): OrderIndexerEventsConsumerFactory {
        val replicaSet = polygonProperties.brokerReplicaSet
        return OrderIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    @Qualifier("polygon.activity.consumer.factory")
    fun polygonActivityConsumerFactory(): EthActivityEventsConsumerFactory {
        val replicaSet = polygonProperties.brokerReplicaSet
        return EthActivityEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    fun polygonItemWorker(@Qualifier("polygon.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory): KafkaConsumerWorker<NftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerGroup(Entity.ITEM), Blockchain.POLYGON)
        val handler = EthereumItemEventHandler(enrichmentItemEventService, BlockchainDto.POLYGON)
        return createBatchedConsumerWorker(consumer, handler, POLYGON, Entity.ITEM, polygonProperties.itemWorkers)
    }

    @Bean
    fun polygonOwnershipWorker(@Qualifier("polygon.nft.consumer.factory") factory: NftIndexerEventsConsumerFactory): KafkaConsumerWorker<NftOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerGroup(Entity.OWNERSHIP), Blockchain.POLYGON)
        val handler = EthereumOwnershipEventHandler(enrichmentOwnershipEventService, BlockchainDto.POLYGON)
        return createBatchedConsumerWorker(
            consumer,
            handler,
            POLYGON,
            Entity.OWNERSHIP,
            polygonProperties.ownershipWorkers
        )
    }

    @Bean
    fun polygonOrderWorker(@Qualifier("polygon.order.consumer.factory") factory: OrderIndexerEventsConsumerFactory): KafkaConsumerWorker<com.rarible.protocol.dto.OrderEventDto> {
        val consumer = factory.createOrderEventsConsumer(consumerGroup(Entity.ORDER), Blockchain.POLYGON)
        val handler = EthereumOrderEventHandler(orderProducer, enrichmentOrderEventService, BlockchainDto.POLYGON)
        return createBatchedConsumerWorker(consumer, handler, POLYGON, Entity.ORDER, polygonProperties.orderWorkers)
    }

    @Bean
    fun polygonActivityWorker(@Qualifier("polygon.activity.consumer.factory") factory: EthActivityEventsConsumerFactory): KafkaConsumerWorker<com.rarible.protocol.dto.ActivityDto> {
        val consumer = factory.createActivityConsumer(consumerGroup(Entity.ACTIVITY), Blockchain.POLYGON)
        val handler = EthereumActivityEventHandler(activityProducer, BlockchainDto.POLYGON)
        return createSingleConsumerWorker(consumer, handler, POLYGON, Entity.ACTIVITY)
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
    fun flowItemWorker(factory: FlowNftIndexerEventsConsumerFactory): KafkaConsumerWorker<FlowNftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerGroup(Entity.ITEM))
        val handler = FlowItemEventHandler(enrichmentItemEventService, BlockchainDto.FLOW)
        return createBatchedConsumerWorker(consumer, handler, FLOW, Entity.ITEM, flowProperties.itemWorkers)
    }

    @Bean
    fun flowOwnershipWorker(factory: FlowNftIndexerEventsConsumerFactory): KafkaConsumerWorker<FlowOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerGroup(Entity.OWNERSHIP))
        val handler = FlowOwnershipEventHandler(enrichmentOwnershipEventService, BlockchainDto.FLOW)
        return createBatchedConsumerWorker(consumer, handler, FLOW, Entity.OWNERSHIP, flowProperties.ownershipWorkers)
    }

    @Bean
    fun flowOrderChangeWorker(factory: FlowNftIndexerEventsConsumerFactory): KafkaConsumerWorker<FlowOrderEventDto> {
        val consumer = factory.createORderEventsConsumer(consumerGroup(Entity.ORDER))
        val handler = FlowOrderEventHandler(orderProducer, enrichmentOrderEventService, BlockchainDto.FLOW)
        return createBatchedConsumerWorker(consumer, handler, FLOW, Entity.ORDER, flowProperties.orderWorkers)
    }

    @Bean
    fun flowActivityWorker(factory: FlowActivityEventsConsumerFactory): KafkaConsumerWorker<FlowActivityDto> {
        val consumer = factory.createActivityConsumer(consumerGroup(Entity.ACTIVITY))
        val handler = FlowActivityEventHandler(activityProducer, BlockchainDto.FLOW)
        return createSingleConsumerWorker(consumer, handler, FLOW, Entity.ACTIVITY)
    }

    private fun <T> createSingleConsumerWorker(
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

    private fun <T> createBatchedConsumerWorker(
        consumer: RaribleKafkaConsumer<T>,
        handler: ConsumerEventHandler<T>,
        blockchain: String,
        entityType: String,
        count: Int
    ): BatchedConsumerWorker<T> {
        val workers = (1..count).map {
            ConsumerWorker(
                consumer = consumer,
                properties = properties.monitoringWorker,
                eventHandler = handler,
                meterRegistry = meterRegistry,
                workerName = "${blockchain}-${entityType}-$it"
            )
        }
        return BatchedConsumerWorker(workers)
    }

    private fun consumerGroup(suffix: String): String {
        return "${env}.protocol.union.${suffix}"
    }
}
