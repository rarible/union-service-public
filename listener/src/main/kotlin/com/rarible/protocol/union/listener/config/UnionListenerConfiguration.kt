package com.rarible.protocol.union.listener.config

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.task.EnableRaribleTask
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.event.UnionInternalTopicProvider
import com.rarible.protocol.union.core.handler.BatchedConsumerWorker
import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.core.model.CompositeRegisteredTimer
import com.rarible.protocol.union.core.model.ItemEventDelayMetric
import com.rarible.protocol.union.core.model.OrderEventDelayMetric
import com.rarible.protocol.union.core.model.OwnershipEventDelayMetric
import com.rarible.protocol.union.core.model.ReconciliationMarkEvent
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.configuration.EnrichmentConsumerConfiguration
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
    private val meterRegistry: MeterRegistry,
    private val ff: FeatureFlagsProperties,
    activeBlockchains: List<BlockchainDto>,
) {

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host
    private val blockchains = activeBlockchains.toSet()

    private val clientIdPrefix = "$env.$host.${UUID.randomUUID()}"
    private val properties = listenerProperties.consumer

    @Deprecated("Replaced by blockchain topics")
    private fun createUnionWrappedEventConsumer(index: Int): RaribleKafkaConsumer<UnionInternalBlockchainEvent> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.union-wrapped-event-consumer-$index",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = UnionInternalBlockchainEvent::class.java,
            consumerGroup = consumerGroup("wrapped"),
            defaultTopic = UnionInternalTopicProvider.getWrappedTopic(env),
            bootstrapServers = properties.brokerReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    @Deprecated("Replaced by blockchain topics")
    fun unionWrappedEventWorker(
        handler: InternalEventHandler<UnionInternalBlockchainEvent>
    ): KafkaConsumerWorker<UnionInternalBlockchainEvent> {
        // Allow to disable this consumers batch
        if (!ff.enableLegacyWrappedEventTopic) return BatchedConsumerWorker(emptyList())

        return consumerFactory.createWrappedEventConsumer(
            consumer = { index -> createUnionWrappedEventConsumer(index) },
            handler = handler,
            daemon = listenerProperties.monitoringWorker,
            workers = properties.workers["wrapped"] ?: 1
        )
    }

    private fun createUnionBlockchainEventConsumer(
        index: Int, blockchain: BlockchainDto
    ): RaribleKafkaConsumer<UnionInternalBlockchainEvent> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.union-blockchain-event-consumer-$index",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = UnionInternalBlockchainEvent::class.java,
            consumerGroup = consumerGroup("blockchain.${blockchain.name.lowercase()}"),
            defaultTopic = UnionInternalTopicProvider.getInternalBlockchainTopic(env, blockchain),
            bootstrapServers = properties.brokerReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun unionBlockchainEventWorker(
        handler: InternalEventHandler<UnionInternalBlockchainEvent>
    ): KafkaConsumerWorker<UnionInternalBlockchainEvent> {
        val consumers = blockchains.map { blockchain ->
            consumerFactory.createInternalBlockchainEventConsumer(
                consumer = { index -> createUnionBlockchainEventConsumer(index, blockchain) },
                handler = handler,
                daemon = listenerProperties.monitoringWorker,
                workers = properties.blockchainWorkers,
                blockchain = blockchain
            )
        }
        val workers = consumers.flatMap { it.workers }
        return BatchedConsumerWorker(workers)
    }

    private fun createUnionReconciliationMarkEventConsumer(
        index: Int
    ): RaribleKafkaConsumer<ReconciliationMarkEvent> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.union-reconciliation-mark-consumer-$index",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = ReconciliationMarkEvent::class.java,
            consumerGroup = consumerGroup("reconciliation"),
            defaultTopic = UnionInternalTopicProvider.getReconciliationMarkTopic(env),
            bootstrapServers = properties.brokerReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun unionReconciliationMarkEventWorker(
        handler: InternalEventHandler<ReconciliationMarkEvent>
    ): KafkaConsumerWorker<ReconciliationMarkEvent> {
        return consumerFactory.createReconciliationMarkEventConsumer(
            consumer = { index -> createUnionReconciliationMarkEventConsumer(index) },
            handler = handler,
            daemon = listenerProperties.monitoringWorker,
            workers = 1
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
