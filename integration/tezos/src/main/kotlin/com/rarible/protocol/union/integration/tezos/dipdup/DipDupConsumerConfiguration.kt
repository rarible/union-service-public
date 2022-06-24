package com.rarible.protocol.union.integration.tezos.dipdup

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.application.ApplicationEnvironmentInfo
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
import com.rarible.protocol.union.integration.tezos.TezosIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupCollectionConverter
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupActivityEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupCollectionEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupOrderEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupTransfersEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktOwnershipService
import org.apache.commons.lang3.StringUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Import

@DipDupConfiguration
@Import(DipDupApiConfiguration::class)
class DipDupConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: TezosIntegrationProperties,
    private val dipDupProperties: DipDupIntegrationProperties,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host
    private val workers = properties.consumer!!.workers

    private val daemon = properties.daemon

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
    @DependsOn("tezosOrderEventHandler")
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
        val consumer = factory.createOrderConsumer(dipdupGroup(consumerFactory.orderGroup))
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
        mapper: ObjectMapper
    ): DipDupActivityEventHandler {
        return DipDupActivityEventHandler(handler, converter, transfersEventHandler, mapper)
    }

    @Bean
    fun dipDupActivityEventWorker(
        factory: DipDupEventsConsumerFactory,
        handler: DipDupActivityEventHandler
    ): KafkaConsumerWorker<DipDupActivity> {
        val consumer = factory.createActivityConsumer(dipdupGroup(consumerFactory.activityGroup))
        return consumerFactory.createActivityConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun dipDupCollectionEventHandler(
        handler: IncomingEventHandler<UnionCollectionEvent>,
        converter: DipDupCollectionConverter,
        mapper: ObjectMapper
    ): DipDupCollectionEventHandler {
        return DipDupCollectionEventHandler(handler, converter, mapper)
    }

    @Bean
    fun dipDupCollectionEventWorker(
        factory: DipDupEventsConsumerFactory,
        handler: DipDupCollectionEventHandler
    ): KafkaConsumerWorker<DipDupCollection> {
        val consumer = factory.createCollectionConsumer(dipdupGroup(consumerFactory.collectionGroup))
        return consumerFactory.createCollectionConsumer(consumer, handler, daemon, workers)
    }

    private fun dipdupGroup(group: String) = "dipdup.$group"
}
