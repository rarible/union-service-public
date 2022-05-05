package com.rarible.protocol.union.integration.tezos.dipdup

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.listener.config.DipDupEventsConsumerFactory
import com.rarible.protocol.union.core.ConsumerFactory
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.integration.tezos.TezosIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupActivityEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupOrderEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.event.DipDupTransfersEventHandler
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktOwnershipService
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Import

@DipDupConfiguration
@Import(DipDupApiConfiguration::class)
class DipDupConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: TezosIntegrationProperties,
    @Value("\${rarible.core.client.k8s:false}")
    private val k8s: Boolean,
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
        val consumer = factory.createOrderConsumer(consumerGroup(consumerFactory.activityGroup))
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
        val consumer = factory.createActivityConsumer(consumerGroup(consumerFactory.activityGroup))
        return consumerFactory.createActivityConsumer(consumer, handler, daemon, workers)
    }

    private fun consumerGroup(group: String): String {
        // Since Tezos has kafka outside our cluster and several envs uses same topics, groupId should be customized
        return if (k8s) {
            "k8s.$group"
        } else {
            group
        }
    }
}
