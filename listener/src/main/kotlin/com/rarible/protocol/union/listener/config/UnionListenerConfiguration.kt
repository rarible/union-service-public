package com.rarible.protocol.union.listener.config

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.task.EnableRaribleTask
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.event.UnionInternalTopicProvider
import com.rarible.protocol.union.core.handler.ConsumerWorkerGroup
import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.core.model.CompositeRegisteredTimer
import com.rarible.protocol.union.core.model.ItemEventDelayMetric
import com.rarible.protocol.union.core.model.OrderEventDelayMetric
import com.rarible.protocol.union.core.model.OwnershipEventDelayMetric
import com.rarible.protocol.union.core.model.ReconciliationMarkEvent
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.configuration.EnrichmentConsumerConfiguration
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.listener.clickhouse.configuration.ClickHouseConfiguration
import com.rarible.protocol.union.listener.downloader.ItemMetaTaskRouter
import com.rarible.protocol.union.listener.handler.internal.ItemMetaTaskSchedulerHandler
import com.rarible.protocol.union.listener.job.BestOrderCheckJob
import com.rarible.protocol.union.listener.job.BestOrderCheckJobHandler
import com.rarible.protocol.union.listener.job.CollectionStatisticsResyncJob
import com.rarible.protocol.union.listener.job.ReconciliationMarkJob
import com.rarible.protocol.union.listener.job.ReconciliationMarkJobHandler
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.util.*

@Configuration
@EnableRaribleTask
@EnableMongock
@Import(value = [EnrichmentConsumerConfiguration::class, ClickHouseConfiguration::class])
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
        if (!ff.enableLegacyWrappedEventTopic) return ConsumerWorkerGroup(emptyList())

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
        return ConsumerWorkerGroup(workers)
    }

    @Bean
    @ConditionalOnProperty(name = ["listener.price-update.enabled"], havingValue = "true")
    fun bestOrderCheckJob(
        handler: BestOrderCheckJobHandler,
        properties: UnionListenerProperties,
        meterRegistry: MeterRegistry,
    ): BestOrderCheckJob {
        return BestOrderCheckJob(handler, properties, meterRegistry)
    }

    @Bean
    @ConditionalOnProperty(name = ["listener.reconcile-marks.enabled"], havingValue = "true")
    fun reconciliationMarkJob(
        handler: ReconciliationMarkJobHandler,
        properties: UnionListenerProperties,
        meterRegistry: MeterRegistry,
    ): ReconciliationMarkJob {
        return ReconciliationMarkJob(handler, properties, meterRegistry)
    }

    @Bean
    @ConditionalOnProperty(name = ["listener.collection-statistics-resync.enabled"], havingValue = "true")
    fun collectionStatisticsResyncJob(
        properties: UnionListenerProperties,
        meterRegistry: MeterRegistry,
        taskRepository: TaskRepository
    ): CollectionStatisticsResyncJob {
        return CollectionStatisticsResyncJob(properties, meterRegistry, taskRepository)
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

    // --------------- Meta 3.0 beans START
    @Bean
    @ConditionalOnProperty("common.feature-flags.enableMetaPipeline", havingValue = "true", matchIfMissing = false)
    fun itemMetaDownloadScheduleWorker(
        handler: ItemMetaTaskSchedulerHandler
    ): ConsumerWorkerGroup<DownloadTask> {
        val properties = listenerProperties.metaScheduling.item
        val consumerGroupSuffix = "meta.item"
        val clientIdSuffix = "item-meta-task-scheduler"
        val topic = UnionInternalTopicProvider.getItemMetaDownloadTaskSchedulerTopic(env)
        return consumerFactory.createInternalBatchedConsumerWorker(
            consumer = { index -> createDownloadTaskConsumer(index, clientIdSuffix, consumerGroupSuffix, topic) },
            handler = handler,
            daemonWorkerProperties = DaemonWorkerProperties(consumerBatchSize = properties.batchSize),
            workers = properties.workers,
            type = "item-meta-task-scheduler"
        )
    }

    private fun createDownloadTaskConsumer(
        index: Int,
        clientIdSuffix: String,
        consumerGroupSuffix: String,
        topic: String
    ): RaribleKafkaConsumer<DownloadTask> {
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.union-$clientIdSuffix-$index",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = DownloadTask::class.java,
            consumerGroup = consumerGroup("download.scheduler.$consumerGroupSuffix"),
            defaultTopic = topic,
            bootstrapServers = properties.brokerReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun itemMetaTaskRouter(): ItemMetaTaskRouter {
        val producers = HashMap<String, RaribleKafkaProducer<DownloadTask>>()
        ItemMetaPipeline.values().map { it.pipeline }.forEach { pipeline ->
            val topic = UnionInternalTopicProvider.getItemMetaDownloadTaskExecutorTopic(env, pipeline)
            val producer = RaribleKafkaProducer(
                clientId = "${env}.protocol-union-service.meta.scheduler",
                valueSerializerClass = UnionKafkaJsonSerializer::class.java,
                valueClass = DownloadTask::class.java,
                defaultTopic = topic,
                bootstrapServers = properties.brokerReplicaSet
            )
            producers[pipeline] = producer
        }
        return ItemMetaTaskRouter(producers)
    }

    // --------------- Meta 3.0 beans END

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
