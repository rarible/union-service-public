package com.rarible.protocol.union.meta.loader.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.core.kafka.RaribleKafkaConsumerWorkerGroup
import com.rarible.protocol.union.core.event.UnionInternalTopicProvider
import com.rarible.protocol.union.core.kafka.KafkaGroupFactory
import com.rarible.protocol.union.core.kafka.KafkaGroupFactory.Companion.COLLECTION_TYPE
import com.rarible.protocol.union.core.kafka.KafkaGroupFactory.Companion.ITEM_TYPE
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
import com.rarible.protocol.union.meta.loader.executor.CollectionDownloadExecutor
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutor
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutorHandler
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutorManager
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutorMetrics
import com.rarible.protocol.union.meta.loader.executor.DownloadPool
import com.rarible.protocol.union.meta.loader.executor.ItemDownloadExecutor
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(value = [UnionMetaLoaderConfiguration::class])
@EnableConfigurationProperties(value = [UnionMetaLoaderProperties::class])
class DownloadExecutorConfiguration(
    private val metaProperties: UnionMetaProperties,
    private val metaLoaderProperties: UnionMetaLoaderProperties,
    private val kafkaGroupFactory: KafkaGroupFactory,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host
    private val kafkaConsumerFactory = RaribleKafkaConsumerFactory(env = env, host = host)

    @Bean
    @Qualifier("item.meta.download.executor.manager")
    fun itemMetaDownloadExecutorManager(
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
    ): RaribleKafkaConsumerWorker<DownloadTask> {
        val workers = ItemMetaPipeline.values().map { it.name.lowercase() }.map { pipeline ->
            val conf = getItemPipelineConfiguration(pipeline)
            val handler = DownloadExecutorHandler(pipeline, executorManager)
            createDownloadExecutorConsumer(ITEM_TYPE, pipeline, conf.workers, conf.batchSize, handler)
        }

        // Just join all pipeline workers into single list
        return RaribleKafkaConsumerWorkerGroup(workers)
    }

    private fun getItemPipelineConfiguration(pipeline: String): ExecutorPipelineProperties {
        val result = metaLoaderProperties.downloader.item[pipeline] ?: ExecutorPipelineProperties()
        logger.info("Settings for Item downloader pipeline '{}': {}", pipeline, result)
        return result
    }

    @Bean
    @Qualifier("collection.meta.download.executor.manager")
    fun collectionMetaDownloadExecutorManager(
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
    ): RaribleKafkaConsumerWorker<DownloadTask> {
        val consumers = CollectionMetaPipeline.values().map { it.name.lowercase() }.map { pipeline ->
            val conf = getCollectionPipelineConfiguration(pipeline)
            val handler = DownloadExecutorHandler(pipeline, executorManager)
            createDownloadExecutorConsumer(COLLECTION_TYPE, pipeline, conf.workers, conf.batchSize, handler)
        }

        // Just join all pipeline workers into single list
        return RaribleKafkaConsumerWorkerGroup(consumers)
    }

    private fun getCollectionPipelineConfiguration(pipeline: String): ExecutorPipelineProperties {
        val result = metaLoaderProperties.downloader.item[pipeline] ?: ExecutorPipelineProperties()
        logger.info("Settings for Collection downloader pipeline '{}': {}", pipeline, result)
        return result
    }

    private fun createDownloadExecutorConsumer(
        type: String,
        pipeline: String,
        concurrency: Int,
        batchSize: Int,
        handler: RaribleKafkaBatchEventHandler<DownloadTask>
    ): RaribleKafkaConsumerWorker<DownloadTask> {
        val topic = when (type) {
            COLLECTION_TYPE -> UnionInternalTopicProvider.getCollectionMetaDownloadTaskExecutorTopic(env, pipeline)
            ITEM_TYPE -> UnionInternalTopicProvider.getItemMetaDownloadTaskExecutorTopic(env, pipeline)
            else -> throw IllegalArgumentException("Unsupported type for meta-loader: $type")
        }
        val settings = RaribleKafkaConsumerSettings(
            hosts = metaLoaderProperties.brokerReplicaSet,
            topic = topic,
            group = kafkaGroupFactory.metaDownloadExecutorGroup(type),
            concurrency = concurrency,
            batchSize = batchSize,
            async = false,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            valueClass = DownloadTask::class.java
        )
        logger.info(
            "Created $concurrency consumers for $type-download-executor (pipeline: $pipeline, batchSize: $batchSize)"
        )
        return kafkaConsumerFactory.createWorker(settings, handler)
    }
}