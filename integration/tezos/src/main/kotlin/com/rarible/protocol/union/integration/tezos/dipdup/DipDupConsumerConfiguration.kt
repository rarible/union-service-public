package com.rarible.protocol.union.integration.tezos.dipdup

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.logging.Logger
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupCollection
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.listener.config.DipDupEventsConsumerFactory
import com.rarible.protocol.union.core.ConsumerFactory
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupCollectionConverter
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupActivityEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupCollectionEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupOrderEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupTransfersEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktCollectionService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktOwnershipService
import org.apache.commons.lang3.StringUtils
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
            dipDupProperties.network,
            dipDupProperties.consumer!!.brokerReplicaSet!!,
            host,
            env,
            StringUtils.trimToNull(dipDupProperties.consumer.username),
            StringUtils.trimToNull(dipDupProperties.consumer.password)
        )
    }

    @Bean
    fun dipDupOrderEventHandler(
        handler: IncomingEventHandler<UnionOrderEvent>,
        converter: DipDupOrderConverter,
        mapper: ObjectMapper
    ): DipDupOrderEventHandler {
        return DipDupOrderEventHandler(handler, converter, mapper)
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
        converter: DipDupCollectionConverter,
        mapper: ObjectMapper,
        tzktCollectionService: TzktCollectionService
    ): DipDupCollectionEventHandler {
        return DipDupCollectionEventHandler(handler, converter, tzktCollectionService, mapper)
    }

    @Bean
    fun dipDupCollectionEventWorker(
        factory: DipDupEventsConsumerFactory,
        handler: DipDupCollectionEventHandler
    ): KafkaConsumerWorker<DipDupCollection> {
        val consumer = factory.createCollectionConsumer(consumerFactory.collectionGroup)
        return consumerFactory.createCollectionConsumer(consumer, handler, daemon, workers)
    }

    companion object {
        private val logger by Logger()
    }
}
