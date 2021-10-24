package com.rarible.protocol.union.integration.flow

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.flow.nft.api.subscriber.FlowNftIndexerEventsConsumerFactory
import com.rarible.protocol.union.core.ConsumerFactory
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.integration.flow.event.FlowActivityEventHandler
import com.rarible.protocol.union.integration.flow.event.FlowItemEventHandler
import com.rarible.protocol.union.integration.flow.event.FlowOrderEventHandler
import com.rarible.protocol.union.integration.flow.event.FlowOwnershipEventHandler
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@FlowComponent
@Import(CoreConfiguration::class)
@ComponentScan(basePackageClasses = [FlowConsumerConfiguration::class])
@EnableConfigurationProperties(value = [FlowIntegrationProperties::class])
@ConditionalOnProperty(name = ["integration.flow.consumer.brokerReplicaSet"])
class FlowConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: FlowIntegrationProperties,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val consumer = properties.consumer!!
    private val workers = properties.consumer!!.workers

    private val daemon = properties.daemon

    @Bean
    fun flowActivityConsumerFactory(): FlowActivityEventsConsumerFactory {
        val replicaSet = consumer.brokerReplicaSet
        return FlowActivityEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    fun flowNftIndexerConsumerFactory(): FlowNftIndexerEventsConsumerFactory {
        val replicaSet = consumer.brokerReplicaSet
        return FlowNftIndexerEventsConsumerFactory(replicaSet, host, env)
    }

    @Bean
    fun flowItemWorker(
        factory: FlowNftIndexerEventsConsumerFactory,
        handler: FlowItemEventHandler
    ): KafkaConsumerWorker<FlowNftItemEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerFactory.itemGroup)
        return consumerFactory.createItemConsumer(consumer, handler, daemon, workers)
    }

    // TODO: Flow will support events on collections => create a worker here.

    @Bean
    fun flowOwnershipWorker(
        factory: FlowNftIndexerEventsConsumerFactory,
        handler: FlowOwnershipEventHandler
    ): KafkaConsumerWorker<FlowOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerFactory.ownershipGroup)
        return consumerFactory.createOwnershipConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun flowOrderWorker(
        factory: FlowNftIndexerEventsConsumerFactory,
        handler: FlowOrderEventHandler
    ): KafkaConsumerWorker<FlowOrderEventDto> {
        val consumer = factory.createORderEventsConsumer(consumerFactory.orderGroup)
        return consumerFactory.createOrderConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun flowActivityWorker(
        factory: FlowActivityEventsConsumerFactory,
        handler: FlowActivityEventHandler
    ): KafkaConsumerWorker<FlowActivityDto> {
        val consumer = factory.createActivityConsumer(consumerFactory.activityGroup)
        return consumerFactory.createActivityConsumer(consumer, handler, daemon, workers)
    }
}