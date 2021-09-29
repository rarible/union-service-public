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
        val handler = EthereumItemEventHandler(enrichmentItemEventService, BlockchainDto.ETHEREUM)
        return createConsumerWorker(consumer, handler, ETHEREUM, Entity.ITEM)
    }

    @Bean
    fun ethereumOwnershipWorker(factory: NftIndexerEventsConsumerFactory): SingleConsumerWorker<NftOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerGroup(Entity.OWNERSHIP), Blockchain.ETHEREUM)
        val handler = EthereumOwnershipEventHandler(enrichmentOwnershipEventService, BlockchainDto.ETHEREUM)
        return createConsumerWorker(consumer, handler, ETHEREUM, Entity.OWNERSHIP)
    }

    @Bean
    fun ethereumOrderWorker(factory: OrderIndexerEventsConsumerFactory): SingleConsumerWorker<com.rarible.protocol.dto.OrderEventDto> {
        val consumer = factory.createOrderEventsConsumer(consumerGroup(Entity.ORDER), Blockchain.ETHEREUM)
        val handler = EthereumOrderEventHandler(orderProducer, enrichmentOrderEventService, BlockchainDto.ETHEREUM)
        return createConsumerWorker(consumer, handler, ETHEREUM, Entity.ORDER)
    }

    @Bean
    fun ethereumActivityWorker(factory: EthActivityEventsConsumerFactory): SingleConsumerWorker<com.rarible.protocol.dto.ActivityDto> {
        val consumer = factory.createActivityConsumer(consumerGroup(Entity.ACTIVITY), Blockchain.ETHEREUM)
        val handler = EthereumActivityEventHandler(activityProducer, BlockchainDto.ETHEREUM)
        return createConsumerWorker(consumer, handler, ETHEREUM, Entity.ACTIVITY)
    }

    // ------ POLYGON
    @Bean
    fun polygonItemWorker(factory: NftIndexerEventsConsumerFactory): SingleConsumerWorker<NftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerGroup(Entity.ITEM), Blockchain.POLYGON)
        val handler = EthereumItemEventHandler(enrichmentItemEventService, BlockchainDto.POLYGON)
        return createConsumerWorker(consumer, handler, POLYGON, Entity.ITEM)
    }

    @Bean
    fun polygonOwnershipWorker(factory: NftIndexerEventsConsumerFactory): SingleConsumerWorker<NftOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerGroup(Entity.OWNERSHIP), Blockchain.POLYGON)
        val handler = EthereumOwnershipEventHandler(enrichmentOwnershipEventService, BlockchainDto.POLYGON)
        return createConsumerWorker(consumer, handler, POLYGON, Entity.OWNERSHIP)
    }

    @Bean
    fun polygonOrderWorker(factory: OrderIndexerEventsConsumerFactory): SingleConsumerWorker<com.rarible.protocol.dto.OrderEventDto> {
        val consumer = factory.createOrderEventsConsumer(consumerGroup(Entity.ORDER), Blockchain.POLYGON)
        val handler = EthereumOrderEventHandler(orderProducer, enrichmentOrderEventService, BlockchainDto.POLYGON)
        return createConsumerWorker(consumer, handler, POLYGON, Entity.ORDER)
    }

    @Bean
    fun polygonActivityWorker(factory: EthActivityEventsConsumerFactory): SingleConsumerWorker<com.rarible.protocol.dto.ActivityDto> {
        val consumer = factory.createActivityConsumer(consumerGroup(Entity.ACTIVITY), Blockchain.POLYGON)
        val handler = EthereumActivityEventHandler(activityProducer, BlockchainDto.POLYGON)
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
        val handler = FlowItemEventHandler(enrichmentItemEventService, BlockchainDto.FLOW)
        return createConsumerWorker(consumer, handler, FLOW, Entity.ITEM)
    }

    @Bean
    fun flowOwnershipWorker(factory: FlowNftIndexerEventsConsumerFactory): SingleConsumerWorker<FlowOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerGroup(Entity.OWNERSHIP))
        val handler = FlowOwnershipEventHandler(enrichmentOwnershipEventService, BlockchainDto.FLOW)
        return createConsumerWorker(consumer, handler, FLOW, Entity.OWNERSHIP)
    }

    @Bean
    fun flowOrderChangeWorker(factory: FlowNftIndexerEventsConsumerFactory): SingleConsumerWorker<FlowOrderEventDto> {
        val consumer = factory.createORderEventsConsumer(consumerGroup(Entity.ORDER))
        val handler = FlowOrderEventHandler(orderProducer, enrichmentOrderEventService, BlockchainDto.FLOW)
        return createConsumerWorker(consumer, handler, FLOW, Entity.ORDER)
    }

    @Bean
    fun flowActivityWorker(factory: FlowActivityEventsConsumerFactory): SingleConsumerWorker<FlowActivityDto> {
        val consumer = factory.createActivityConsumer(consumerGroup(Entity.ACTIVITY))
        val handler = FlowActivityEventHandler(activityProducer, BlockchainDto.FLOW)
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
}
