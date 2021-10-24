package com.rarible.protocol.union.integration.tezos

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.tezos.dto.ActivityDto
import com.rarible.protocol.tezos.dto.ItemEventDto
import com.rarible.protocol.tezos.dto.OrderEventDto
import com.rarible.protocol.tezos.dto.OwnershipEventDto
import com.rarible.protocol.tezos.subscriber.TezosEventsConsumerFactory
import com.rarible.protocol.union.core.ConsumerFactory
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.integration.tezos.event.TezosActivityEventHandler
import com.rarible.protocol.union.integration.tezos.event.TezosItemEventHandler
import com.rarible.protocol.union.integration.tezos.event.TezosOrderEventHandler
import com.rarible.protocol.union.integration.tezos.event.TezosOwnershipEventHandler
import org.apache.commons.lang3.StringUtils
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@TezosComponent
@Import(CoreConfiguration::class)
@ComponentScan(basePackageClasses = [TezosConsumerConfiguration::class])
@EnableConfigurationProperties(value = [TezosIntegrationProperties::class])
@ConditionalOnProperty(name = ["integration.tezos.consumer.brokerReplicaSet"])
class TezosConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: TezosIntegrationProperties,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val consumer = properties.consumer!!
    private val workers = properties.consumer!!.workers

    private val daemon = properties.daemon

    @Bean
    fun tezosConsumerFactory(): TezosEventsConsumerFactory {
        return TezosEventsConsumerFactory(
            consumer.brokerReplicaSet,
            host,
            env,
            StringUtils.trimToNull(consumer.username),
            StringUtils.trimToNull(consumer.password)
        )
    }

    @Bean
    fun tezosItemWorker(
        factory: TezosEventsConsumerFactory,
        handler: TezosItemEventHandler
    ): KafkaConsumerWorker<ItemEventDto> {
        val consumer = factory.createItemConsumer(consumerFactory.itemGroup)
        return consumerFactory.createItemConsumer(consumer, handler, daemon, workers)
    }

    // TODO: Tezos will support events on collections => create a worker here.

    @Bean
    fun tezosOwnershipWorker(
        factory: TezosEventsConsumerFactory,
        handler: TezosOwnershipEventHandler
    ): KafkaConsumerWorker<OwnershipEventDto> {
        val consumer = factory.createOwnershipConsumer(consumerFactory.ownershipGroup)
        return consumerFactory.createOwnershipConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun tezosOrderWorker(
        factory: TezosEventsConsumerFactory,
        handler: TezosOrderEventHandler
    ): KafkaConsumerWorker<OrderEventDto> {
        val consumer = factory.createOrderConsumer(consumerFactory.orderGroup)
        return consumerFactory.createOrderConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun tezosActivityWorker(
        factory: TezosEventsConsumerFactory,
        handler: TezosActivityEventHandler
    ): KafkaConsumerWorker<ActivityDto> {
        val consumer = factory.createActivityConsumer(consumerFactory.activityGroup)
        return consumerFactory.createActivityConsumer(consumer, handler, daemon, workers)
    }
}