package com.rarible.protocol.union.integration.flow

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.flow.nft.api.subscriber.FlowNftIndexerEventsConsumerFactory
import com.rarible.protocol.union.core.event.ConsumerFactory
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.core.model.UnionActivityDto
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.integration.flow.converter.FlowActivityConverter
import com.rarible.protocol.union.integration.flow.converter.FlowOrderConverter
import com.rarible.protocol.union.integration.flow.event.FlowActivityEventHandler
import com.rarible.protocol.union.integration.flow.event.FlowItemEventHandler
import com.rarible.protocol.union.integration.flow.event.FlowOrderEventHandler
import com.rarible.protocol.union.integration.flow.event.FlowOwnershipEventHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@FlowConfiguration
@Import(FlowApiConfiguration::class)
class FlowConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: FlowIntegrationProperties,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val consumer = properties.consumer!!
    private val daemon = properties.daemon

    private val workers = consumer.workers
    private val batchSize = consumer.batchSize

    //-------------------- Handlers -------------------//

    @Bean
    fun flowItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): FlowItemEventHandler {
        return FlowItemEventHandler(handler)
    }

    @Bean
    fun flowOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): FlowOwnershipEventHandler {
        return FlowOwnershipEventHandler(handler)
    }

    // TODO FLOW not supported yet
    /*
    @Bean
    fun flowCollectionEventHandler(handler: IncomingEventHandler<CollectionEventDto>): FlowCollectionEventHandler {
        return FlowCollectionEventHandler(handler)
    }
     */

    @Bean
    fun flowOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: FlowOrderConverter
    ): FlowOrderEventHandler {
        return FlowOrderEventHandler(handler, converter)
    }

    @Bean
    fun flowActivityEventHandler(
        handler: IncomingEventHandler<UnionActivityDto>,
        converter: FlowActivityConverter
    ): FlowActivityEventHandler {
        return FlowActivityEventHandler(handler, converter)
    }

    //-------------------- Workers --------------------//

    @Bean
    fun flowNftIndexerConsumerFactory(): FlowNftIndexerEventsConsumerFactory {
        val replicaSet = consumer.brokerReplicaSet
        return FlowNftIndexerEventsConsumerFactory(replicaSet!!, host, env)
    }

    @Bean
    fun flowItemWorker(
        factory: FlowNftIndexerEventsConsumerFactory,
        handler: BlockchainEventHandler<FlowNftItemEventDto, UnionItemEvent>
    ): KafkaConsumerWorker<FlowNftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerFactory.itemGroup)
        return consumerFactory.createItemConsumer(consumer, handler, daemon, workers, batchSize)
    }

    // TODO: Flow will support events on collections => create a worker here.

    @Bean
    fun flowOwnershipWorker(
        factory: FlowNftIndexerEventsConsumerFactory,
        handler: BlockchainEventHandler<FlowOwnershipEventDto, UnionOwnershipEvent>
    ): KafkaConsumerWorker<FlowOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerFactory.ownershipGroup)
        return consumerFactory.createOwnershipConsumer(consumer, handler, daemon, workers, batchSize)
    }

    @Bean
    fun flowOrderWorker(
        factory: FlowNftIndexerEventsConsumerFactory,
        handler: BlockchainEventHandler<FlowOrderEventDto, UnionOrderEvent>
    ): KafkaConsumerWorker<FlowOrderEventDto> {
        val consumer = factory.createOrderEventsConsumer(consumerFactory.orderGroup)
        return consumerFactory.createOrderConsumer(consumer, handler, daemon, workers, batchSize)
    }

    @Bean
    fun flowActivityWorker(
        factory: FlowNftIndexerEventsConsumerFactory,
        handler: BlockchainEventHandler<FlowActivityDto, UnionActivityDto>
    ): KafkaConsumerWorker<FlowActivityDto> {
        val consumer = factory.createAcitivityEventsConsumer(consumerFactory.activityGroup)
        return consumerFactory.createActivityConsumer(consumer, handler, daemon, workers, batchSize)
    }
}
