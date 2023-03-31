package com.rarible.protocol.union.listener.config

import com.github.cloudyrock.spring.v5.EnableMongock
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.task.EnableRaribleTask
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
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.listener.downloader.MetaTaskRouter
import com.rarible.protocol.union.listener.handler.internal.CollectionMetaTaskSchedulerHandler
import com.rarible.protocol.union.listener.handler.internal.ItemMetaTaskSchedulerHandler
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.beans.factory.annotation.Qualifier
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

    @Bean
    @Qualifier("item.meta.schedule.router")
    fun itemMetaTaskRouter(): MetaTaskRouter {
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
        return MetaTaskRouter(producers)
    }

    @Bean
    fun collectionMetaDownloadScheduleWorker(
        handler: CollectionMetaTaskSchedulerHandler
    ): ConsumerWorkerGroup<DownloadTask> {
        val properties = listenerProperties.metaScheduling.collection
        val consumerGroupSuffix = "meta.collection"
        val clientIdSuffix = "collection-meta-task-scheduler"
        val topic = UnionInternalTopicProvider.getCollectionMetaDownloadTaskSchedulerTopic(env)
        return consumerFactory.createInternalBatchedConsumerWorker(
            consumer = { index -> createDownloadTaskConsumer(index, clientIdSuffix, consumerGroupSuffix, topic) },
            handler = handler,
            daemonWorkerProperties = DaemonWorkerProperties(consumerBatchSize = properties.batchSize),
            workers = properties.workers,
            type = "collection-meta-task-scheduler"
        )
    }

    @Bean
    @Qualifier("collection.meta.schedule.router")
    fun collectionMetaTaskRouter(): MetaTaskRouter {
        val producers = HashMap<String, RaribleKafkaProducer<DownloadTask>>()
        CollectionMetaPipeline.values().map { it.pipeline }.forEach { pipeline ->
            val topic = UnionInternalTopicProvider.getCollectionMetaDownloadTaskExecutorTopic(env, pipeline)
            val producer = RaribleKafkaProducer(
                clientId = "${env}.protocol-union-service.meta.scheduler",
                valueSerializerClass = UnionKafkaJsonSerializer::class.java,
                valueClass = DownloadTask::class.java,
                defaultTopic = topic,
                bootstrapServers = properties.brokerReplicaSet
            )
            producers[pipeline] = producer
        }
        return MetaTaskRouter(producers)
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
