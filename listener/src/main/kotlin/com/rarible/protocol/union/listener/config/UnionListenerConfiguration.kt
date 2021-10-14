package com.rarible.protocol.union.listener.config

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.task.EnableRaribleTask
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
import com.rarible.protocol.tezos.subscriber.TezosEventsConsumerFactory
import com.rarible.protocol.union.core.Entity
import com.rarible.protocol.union.core.ethereum.converter.EthOrderEventConverter
import com.rarible.protocol.union.core.flow.converter.FlowOrderEventConverter
import com.rarible.protocol.union.core.tezos.converter.TezosOrderConverter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.enrichment.configuration.EnrichmentConfiguration
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
import com.rarible.protocol.union.listener.handler.tezos.TezosItemEventHandler
import com.rarible.protocol.union.listener.handler.tezos.TezosOrderEventHandler
import com.rarible.protocol.union.listener.handler.tezos.TezosOwnershipEventHandler
import com.rarible.protocol.union.listener.service.EnrichmentItemEventService
import com.rarible.protocol.union.listener.service.EnrichmentOrderEventService
import com.rarible.protocol.union.listener.service.EnrichmentOwnershipEventService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(EnrichmentConfiguration::class)
@EnableRaribleTask
@EnableMongock
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
        private val TEZOS = BlockchainDto.TEZOS.name.toLowerCase()
    }

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val ethereumProperties = properties.consumer.ethereum
    private val polygonProperties = properties.consumer.polygon
    private val flowProperties = properties.consumer.flow
    private val tezosProperties = properties.consumer.tezos

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
    fun ethereumOrderWorker(
        @Qualifier("ethereum.order.consumer.factory") factory: OrderIndexerEventsConsumerFactory,
        ethOrderEventConverter: EthOrderEventConverter
    ): KafkaConsumerWorker<com.rarible.protocol.dto.OrderEventDto> {
        val consumer = factory.createOrderEventsConsumer(consumerGroup(Entity.ORDER), Blockchain.ETHEREUM)
        val handler = EthereumOrderEventHandler(
            orderProducer,
            enrichmentOrderEventService,
            ethOrderEventConverter,
            BlockchainDto.ETHEREUM
        )
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
    fun polygonOrderWorker(
        @Qualifier("polygon.order.consumer.factory") factory: OrderIndexerEventsConsumerFactory,
        ethOrderEventConverter: EthOrderEventConverter
    ): KafkaConsumerWorker<com.rarible.protocol.dto.OrderEventDto> {
        val consumer = factory.createOrderEventsConsumer(consumerGroup(Entity.ORDER), Blockchain.POLYGON)
        val handler = EthereumOrderEventHandler(
            orderProducer,
            enrichmentOrderEventService,
            ethOrderEventConverter,
            BlockchainDto.POLYGON
        )
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
        val replicaSet = flowProperties.brokerReplicaSet
        return FlowActivityEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    fun flowNftIndexerConsumerFactory(): FlowNftIndexerEventsConsumerFactory {
        val replicaSet = flowProperties.brokerReplicaSet
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
    fun flowOrderWorker(
        factory: FlowNftIndexerEventsConsumerFactory,
        flowOrderEventConverter: FlowOrderEventConverter
    ): KafkaConsumerWorker<FlowOrderEventDto> {
        val consumer = factory.createORderEventsConsumer(consumerGroup(Entity.ORDER))
        val handler = FlowOrderEventHandler(
            orderProducer,
            enrichmentOrderEventService,
            flowOrderEventConverter,
            BlockchainDto.FLOW
        )
        return createBatchedConsumerWorker(consumer, handler, FLOW, Entity.ORDER, flowProperties.orderWorkers)
    }

    @Bean
    fun flowActivityWorker(factory: FlowActivityEventsConsumerFactory): KafkaConsumerWorker<FlowActivityDto> {
        val consumer = factory.createActivityConsumer(consumerGroup(Entity.ACTIVITY))
        val handler = FlowActivityEventHandler(activityProducer, BlockchainDto.FLOW)
        return createSingleConsumerWorker(consumer, handler, FLOW, Entity.ACTIVITY)
    }

    //------------------------ Tezos Consumers ------------------------//
    @Bean
    fun tezosActivityConsumerFactory(): TezosEventsConsumerFactory {
        val replicaSet = tezosProperties.brokerReplicaSet
        return TezosEventsConsumerFactory(replicaSet, host, env, tezosProperties.username, tezosProperties.password)
    }

    @Bean
    fun tezosItemWorker(factory: TezosEventsConsumerFactory): KafkaConsumerWorker<com.rarible.protocol.tezos.dto.ItemEventDto> {
        val consumer = factory.createItemConsumer(consumerGroup(Entity.ITEM))
        val handler = TezosItemEventHandler(enrichmentItemEventService, BlockchainDto.TEZOS)
        return createBatchedConsumerWorker(consumer, handler, TEZOS, Entity.ITEM, tezosProperties.itemWorkers)
    }

    @Bean
    fun tezosOwnershipWorker(factory: TezosEventsConsumerFactory): KafkaConsumerWorker<com.rarible.protocol.tezos.dto.OwnershipEventDto> {
        val consumer = factory.createOwnershipConsumer(consumerGroup(Entity.OWNERSHIP))
        val handler = TezosOwnershipEventHandler(enrichmentOwnershipEventService, BlockchainDto.TEZOS)
        return createBatchedConsumerWorker(consumer, handler, TEZOS, Entity.OWNERSHIP, tezosProperties.ownershipWorkers)
    }

    @Bean
    fun tezosOrderWorker(
        factory: TezosEventsConsumerFactory,
        tezosOrderConverter: TezosOrderConverter
    ): KafkaConsumerWorker<com.rarible.protocol.tezos.dto.OrderEventDto> {
        val consumer = factory.createOrderConsumer(consumerGroup(Entity.ORDER))
        val handler = TezosOrderEventHandler(
            orderProducer,
            enrichmentOrderEventService,
            tezosOrderConverter,
            BlockchainDto.TEZOS
        )
        return createBatchedConsumerWorker(consumer, handler, TEZOS, Entity.ORDER, tezosProperties.orderWorkers)
    }

    /*
    @Bean
    fun tezosActivityWorker(factory: TezosEventsConsumerFactory): KafkaConsumerWorker<TezosActivityDto> {
        val consumer = factory.createActivityConsumer(consumerGroup(Entity.ACTIVITY))
        val handler = TezosActivityEventHandler(activityProducer, BlockchainDto.TEZOS)
        return createSingleConsumerWorker(consumer, handler, TEZOS, Entity.ACTIVITY)
    }
    */

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
