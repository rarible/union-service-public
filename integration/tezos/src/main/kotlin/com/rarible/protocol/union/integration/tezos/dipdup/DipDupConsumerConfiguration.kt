package com.rarible.protocol.union.integration.tezos.dipdup

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.logging.Logger
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.listener.config.DipDupEventsConsumerFactory
import com.rarible.dipdup.listener.model.DipDupCollectionEvent
import com.rarible.dipdup.listener.model.DipDupItemEvent
import com.rarible.dipdup.listener.model.DipDupItemMetaEvent
import com.rarible.dipdup.listener.model.DipDupOwnershipEvent
import com.rarible.protocol.union.core.ConsumerFactory
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupActivityEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupCollectionEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupItemEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupItemMetaEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupOrderEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupOwnershipEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupTransfersEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktCollectionService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktOwnershipService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@DipDupConfiguration
@Import(DipDupApiConfiguration::class)
class DipDupConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val dipDupProperties: DipDupIntegrationProperties,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host
    private val workers = dipDupProperties.consumer!!.workers

    private val daemon = dipDupProperties.daemon

    @Bean
    fun dipDupConsumerFactory(): DipDupEventsConsumerFactory {
        return DipDupEventsConsumerFactory(
            dipDupProperties.consumer!!.brokerReplicaSet!!,
            host,
            env
        )
    }

    @Bean
    fun dipDupOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: DipDupOrderConverter,
        mapper: ObjectMapper
    ): DipDupOrderEventHandler {
        return DipDupOrderEventHandler(handler, converter, mapper, dipDupProperties.marketplaces)
    }

    @Bean
    fun dipDupOrderEventWorker(
        factory: DipDupEventsConsumerFactory,
        handler: DipDupOrderEventHandler
    ): KafkaConsumerWorker<DipDupOrder> {
        val consumer = factory.createOrderConsumer(consumerFactory.orderGroup)
        return consumerFactory.createOrderConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun dipDupTransferEventHandler(
        ownershipHandler: IncomingEventHandler<UnionOwnershipEvent>,
        ownershipService: TzktOwnershipService,
        itemHandler: IncomingEventHandler<UnionItemEvent>,
        itemService: TzktItemService
    ): DipDupTransfersEventHandler {
        return DipDupTransfersEventHandler(ownershipHandler, ownershipService, itemHandler, itemService)
    }

    @Bean
    fun dipDupActivityEventHandler(
        handler: IncomingEventHandler<ActivityDto>,
        converter: DipDupActivityConverter,
        transfersEventHandler: DipDupTransfersEventHandler,
        properties: DipDupIntegrationProperties,
        mapper: ObjectMapper
    ): DipDupActivityEventHandler {
        return DipDupActivityEventHandler(handler, converter, transfersEventHandler, properties, mapper)
    }

    @Bean
    fun dipDupActivityEventWorker(
        factory: DipDupEventsConsumerFactory,
        handler: DipDupActivityEventHandler
    ): KafkaConsumerWorker<DipDupActivity> {
        val consumer = factory.createActivityConsumer(consumerFactory.activityGroup)
        logger.info("Use ${workers} worker config for listening dipdup events")
        return consumerFactory.createActivityConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun dipDupCollectionEventHandler(
        handler: IncomingEventHandler<UnionCollectionEvent>,
        mapper: ObjectMapper,
        tzktCollectionService: TzktCollectionService,
        properties: DipDupIntegrationProperties
    ): DipDupCollectionEventHandler {
        return DipDupCollectionEventHandler(handler, tzktCollectionService, mapper, properties)
    }

    @Bean
    fun dipDupCollectionEventWorker(
        factory: DipDupEventsConsumerFactory,
        handler: DipDupCollectionEventHandler
    ): KafkaConsumerWorker<DipDupCollectionEvent> {
        val consumer = factory.createCollectionConsumer(consumerFactory.collectionGroup)
        return consumerFactory.createCollectionConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun dipDupItemEventHandler(
        handler: IncomingEventHandler<UnionItemEvent>,
        mapper: ObjectMapper
    ): DipDupItemEventHandler {
        return DipDupItemEventHandler(handler, mapper)
    }

    @Bean
    fun dipDupItemEventWorker(
        factory: DipDupEventsConsumerFactory,
        handler: DipDupItemEventHandler
    ): KafkaConsumerWorker<DipDupItemEvent> {
        val consumer = factory.createItemConsumer(consumerFactory.itemGroup)
        return consumerFactory.createItemConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun dipDupItemMetaEventHandler(handler: IncomingEventHandler<UnionItemMetaEvent>): DipDupItemMetaEventHandler {
        return DipDupItemMetaEventHandler(handler)
    }

    @Bean
    fun dipDupItemMetaEventWorker(
        factory: DipDupEventsConsumerFactory,
        handler: BlockchainEventHandler<DipDupItemMetaEvent, UnionItemMetaEvent>
    ): KafkaConsumerWorker<DipDupItemMetaEvent> {
        val consumer = factory.createItemMetaConsumer(consumerFactory.itemMetaGroup)
        return consumerFactory.createItemMetaConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun dipDupOwnershipEventHandler(
        handler: IncomingEventHandler<UnionOwnershipEvent>,
        mapper: ObjectMapper
    ): DipDupOwnershipEventHandler {
        return DipDupOwnershipEventHandler(handler, mapper)
    }

    @Bean
    fun dipDupOwnershipEventWorker(
        factory: DipDupEventsConsumerFactory,
        handler: DipDupOwnershipEventHandler
    ): KafkaConsumerWorker<DipDupOwnershipEvent> {
        val consumer = factory.createOwnershipConsumer(consumerFactory.itemGroup)
        return consumerFactory.createOwnershipConsumer(consumer, handler, daemon, workers)
    }

    companion object {
        private val logger by Logger()
    }
}
