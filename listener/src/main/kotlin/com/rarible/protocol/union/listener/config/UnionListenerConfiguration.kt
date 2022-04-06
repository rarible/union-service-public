package com.rarible.protocol.union.listener.config

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.task.EnableRaribleTask
import com.rarible.protocol.union.core.event.UnionInternalTopicProvider
import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.core.model.*
import com.rarible.protocol.union.enrichment.configuration.EnrichmentConsumerConfiguration
import com.rarible.protocol.union.enrichment.model.ReconciliationMarkAbstractEvent
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.util.*

@Configuration
@EnableRaribleTask
@EnableMongock
@Import(value = [EnrichmentConsumerConfiguration::class])
@EnableConfigurationProperties(value = [UnionListenerProperties::class])
class UnionListenerConfiguration(
    private val listenerProperties: UnionListenerProperties,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val consumerFactory: InternalConsumerFactory,
    private val meterRegistry: MeterRegistry
) {

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val clientIdPrefix = "$env.$host.${UUID.randomUUID()}"

    private fun createUnionWrappedEventConsumer(index: Int): RaribleKafkaConsumer<UnionWrappedEvent> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.union-wrapped-event-consumer-$index",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = UnionWrappedEvent::class.java,
            consumerGroup = consumerGroup("wrapped"),
            defaultTopic = UnionInternalTopicProvider.getWrappedTopic(env),
            bootstrapServers = listenerProperties.consumer.brokerReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun unionWrappedEventWorker(
        handler: InternalEventHandler<UnionWrappedEvent>
    ): KafkaConsumerWorker<UnionWrappedEvent> {
        return consumerFactory.createWrappedEventConsumer(
            consumer = { index -> createUnionWrappedEventConsumer(index) },
            handler = handler,
            daemon = listenerProperties.monitoringWorker,
            workers = listenerProperties.consumer.workers
        )
    }

    private fun createUnionReconciliationMarkEventConsumer(
        index: Int
    ): RaribleKafkaConsumer<ReconciliationMarkAbstractEvent> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.union-reconciliation-mark-consumer-$index",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = ReconciliationMarkAbstractEvent::class.java,
            consumerGroup = consumerGroup("reconciliation"),
            defaultTopic = UnionInternalTopicProvider.getReconciliationMarkTopic(env),
            bootstrapServers = listenerProperties.consumer.brokerReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun unionReconciliationMarkEventWorker(
        handler: InternalEventHandler<ReconciliationMarkAbstractEvent>
    ): KafkaConsumerWorker<ReconciliationMarkAbstractEvent> {
        return consumerFactory.createReconciliationMarkEventConsumer(
            consumer = { index -> createUnionReconciliationMarkEventConsumer(index) },
            handler = handler,
            daemon = listenerProperties.monitoringWorker,
            workerCount = 1
        )
    }

    private fun consumerGroup(suffix: String): String {
        return "${env}.protocol.union.${suffix}"
    }

    @Bean
    fun itemCompositeRegisteredTimer(): CompositeRegisteredTimer {
        return ItemEventDelayMetric(listenerProperties.metrics.rootPath).bind(meterRegistry)
    }

    @Bean
    fun ownershipCompositeRegisteredTimer(): CompositeRegisteredTimer {
        return OwnershipEventDelayMetric(listenerProperties.metrics.rootPath).bind(meterRegistry)
    }

    @Bean
    fun orderCompositeRegisteredTimer(): CompositeRegisteredTimer {
        return OrderEventDelayMetric(listenerProperties.metrics.rootPath).bind(meterRegistry)
    }
}
