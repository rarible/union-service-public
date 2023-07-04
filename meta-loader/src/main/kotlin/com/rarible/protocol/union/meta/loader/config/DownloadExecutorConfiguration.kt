package com.rarible.protocol.union.meta.loader.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.ConsumerBatchWorker
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.protocol.union.core.event.UnionInternalTopicProvider
import com.rarible.protocol.union.core.handler.ConsumerWorkerGroup
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaDownloader
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaNotifier
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaNotifier
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.repository.CollectionMetaRepository
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentBlacklistService
import com.rarible.protocol.union.meta.loader.executor.CollectionDownloadExecutor
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutor
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutorHandler
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutorManager
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutorMetrics
import com.rarible.protocol.union.meta.loader.executor.DownloadPool
import com.rarible.protocol.union.meta.loader.executor.ItemDownloadExecutor
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.util.UUID

@Configuration
@Import(value = [UnionMetaLoaderConfiguration::class])
@EnableConfigurationProperties(value = [UnionMetaLoaderProperties::class])
class DownloadExecutorConfiguration(
    private val metaProperties: UnionMetaProperties,
    private val metaLoaderProperties: UnionMetaLoaderProperties,
    private val meterRegistry: MeterRegistry,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val clientIdPrefix = "$env.$host.${UUID.randomUUID()}"

    companion object {

        const val COLLECTION_TYPE = "collection"
        const val ITEM_TYPE = "item"
    }

    @Bean
    @Qualifier("item.meta.download.executor.manager")
    fun itemMetaDownloadExecutorManager(
        enrichmentBlacklistService: EnrichmentBlacklistService,
        itemMetaRepository: ItemMetaRepository,
        itemMetaDownloader: ItemMetaDownloader,
        itemMetaNotifier: ItemMetaNotifier,
        itemDownloadExecutorMetrics: DownloadExecutorMetrics
    ): DownloadExecutorManager {
        val maxRetries = metaProperties.retryIntervals.size
        val executors = HashMap<String, DownloadExecutor<UnionMeta>>()
        ItemMetaPipeline.values().map { it.name.lowercase() }.forEach { pipeline ->
            val conf = getItemPipelineConfiguration(pipeline)
            val pool = DownloadPool(conf.poolSize, "item-meta-task-executor")
            val executor = ItemDownloadExecutor(
                enrichmentBlacklistService,
                itemMetaRepository,
                itemMetaDownloader,
                itemMetaNotifier,
                pool,
                itemDownloadExecutorMetrics,
                maxRetries
            )
            executors[pipeline] = executor
            logger.info(
                "Created item-meta-task-executor (pipeline: $pipeline, poolSize: ${conf.poolSize})"
            )
        }
        return DownloadExecutorManager(executors)
    }

    @Bean
    fun itemMetaDownloadTaskConsumer(
        @Qualifier("item.meta.download.executor.manager")
        executorManager: DownloadExecutorManager
    ): ConsumerWorkerGroup<DownloadTask> {
        val type = ITEM_TYPE
        val workers = ItemMetaPipeline.values().map { it.name.lowercase() }.map { pipeline ->
            val conf = getItemPipelineConfiguration(pipeline)
            val handler = DownloadExecutorHandler(pipeline, executorManager)
            createMetaDownloaderBatchConsumer(pipeline, conf.workers, conf.batchSize, type, handler)
        }.map { it.workers }.flatten()

        // Just join all pipeline workers into single list
        return ConsumerWorkerGroup(workers)
    }

    private fun getItemPipelineConfiguration(pipeline: String): ExecutorPipelineProperties {
        val result = metaLoaderProperties.downloader.item[pipeline] ?: ExecutorPipelineProperties()
        logger.info("Settings for Item downloader pipeline '{}': {}", pipeline, result)
        return result
    }

    @Bean
    @Qualifier("collection.meta.download.executor.manager")
    fun collectionMetaDownloadExecutorManager(
        enrichmentBlacklistService: EnrichmentBlacklistService,
        collectionMetaRepository: CollectionMetaRepository,
        collectionMetaDownloader: CollectionMetaDownloader,
        collectionMetaNotifier: CollectionMetaNotifier,
        collectionDownloadExecutorMetrics: DownloadExecutorMetrics
    ): DownloadExecutorManager {
        val maxRetries = metaProperties.retryIntervals.size
        val executors = HashMap<String, DownloadExecutor<UnionCollectionMeta>>()
        CollectionMetaPipeline.values().map { it.name.lowercase() }.forEach { pipeline ->
            val conf = getCollectionPipelineConfiguration(pipeline)
            val pool = DownloadPool(conf.poolSize, "collection-meta-task-executor")
            val executor = CollectionDownloadExecutor(
                enrichmentBlacklistService,
                collectionMetaRepository,
                collectionMetaDownloader,
                collectionMetaNotifier,
                pool,
                collectionDownloadExecutorMetrics,
                maxRetries
            )
            executors[pipeline] = executor
            logger.info(
                "Created collection-meta-task-executor (pipeline: $pipeline, poolSize: ${conf.poolSize})"
            )
        }
        return DownloadExecutorManager(executors)
    }

    @Bean
    fun collectionMetaDownloadTaskConsumer(
        @Qualifier("collection.meta.download.executor.manager")
        executorManager: DownloadExecutorManager
    ): ConsumerWorkerGroup<DownloadTask> {
        val type = COLLECTION_TYPE
        val workers = CollectionMetaPipeline.values().map { it.name.lowercase() }.map { pipeline ->
            val conf = getCollectionPipelineConfiguration(pipeline)
            val handler = DownloadExecutorHandler(pipeline, executorManager)
            createMetaDownloaderBatchConsumer(pipeline, conf.workers, conf.batchSize, type, handler)
        }.map { it.workers }.flatten()

        // Just join all pipeline workers into single list
        return ConsumerWorkerGroup(workers)
    }

    private fun getCollectionPipelineConfiguration(pipeline: String): ExecutorPipelineProperties {
        val result = metaLoaderProperties.downloader.item[pipeline] ?: ExecutorPipelineProperties()
        logger.info("Settings for Collection downloader pipeline '{}': {}", pipeline, result)
        return result
    }

    private fun createMetaDownloaderBatchConsumer(
        pipeline: String,
        workers: Int,
        batchSize: Int,
        type: String,
        handler: DownloadExecutorHandler
    ): ConsumerWorkerGroup<UnionMeta> {
        val consumerGroupSuffix = "meta.$type"
        val clientIdSuffix = "$type-meta-task-executor"
        val workerSet = (1..workers).map {
            ConsumerBatchWorker(
                consumer = createDownloadExecutorConsumer(it, clientIdSuffix, consumerGroupSuffix, type, pipeline),
                properties = DaemonWorkerProperties(consumerBatchSize = batchSize),
                eventHandler = handler,
                meterRegistry = meterRegistry,
                workerName = "internal-$type-meta-task-executor-$it"
            )
        }
        logger.info(
            "Created $workers consumers for $type-download-executor (pipeline: $pipeline, batchSize: $batchSize)"
        )
        return ConsumerWorkerGroup(workerSet)
    }

    private fun createDownloadExecutorConsumer(
        index: Int,
        clientIdSuffix: String,
        consumerGroupSuffix: String,
        type: String,
        pipeline: String
    ): RaribleKafkaConsumer<DownloadTask> {
        val topic = when (type) {
            COLLECTION_TYPE -> UnionInternalTopicProvider.getCollectionMetaDownloadTaskExecutorTopic(env, pipeline)
            ITEM_TYPE -> UnionInternalTopicProvider.getItemMetaDownloadTaskExecutorTopic(env, pipeline)
            else -> throw IllegalArgumentException("Unsupported type for meta-loader: $type")
        }
        return RaribleKafkaConsumer(
            clientId = "$clientIdPrefix.union-$clientIdSuffix.$pipeline-$index",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = DownloadTask::class.java,
            consumerGroup = consumerGroup("download.executor.$consumerGroupSuffix"),
            defaultTopic = topic,
            bootstrapServers = metaLoaderProperties.brokerReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    private fun consumerGroup(suffix: String): String {
        return "${env}.protocol.union.${suffix}"
    }
}