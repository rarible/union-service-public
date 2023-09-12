package com.rarible.protocol.union.meta.loader.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.core.kafka.RaribleKafkaConsumerFactory
import com.rarible.core.kafka.RaribleKafkaConsumerSettings
import com.rarible.core.kafka.RaribleKafkaContainerFactorySettings
import com.rarible.core.kafka.RaribleKafkaListenerContainerFactory
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.event.UnionInternalTopicProvider
import com.rarible.protocol.union.core.kafka.KafkaGroupFactory
import com.rarible.protocol.union.core.kafka.KafkaGroupFactory.Companion.COLLECTION_TYPE
import com.rarible.protocol.union.core.kafka.KafkaGroupFactory.Companion.ITEM_TYPE
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaDownloader
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaNotifier
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaNotifier
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaRefreshService
import com.rarible.protocol.union.enrichment.meta.item.PartialItemMetaDownloader
import com.rarible.protocol.union.enrichment.repository.CollectionMetaRepository
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import com.rarible.protocol.union.enrichment.service.DownloadTaskService
import com.rarible.protocol.union.enrichment.service.EnrichmentBlacklistService
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.meta.loader.executor.CollectionDownloadExecutor
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutor
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutorHandler
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutorManager
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutorMetrics
import com.rarible.protocol.union.meta.loader.executor.DownloadPool
import com.rarible.protocol.union.meta.loader.executor.ItemDownloadExecutor
import com.rarible.protocol.union.meta.loader.job.DownloadExecutorJob
import com.rarible.protocol.union.meta.loader.job.DownloadExecutorWorker
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@Configuration
@Import(value = [UnionMetaLoaderConfiguration::class])
@EnableConfigurationProperties(value = [UnionMetaLoaderProperties::class])
class DownloadExecutorConfiguration(
    private val metaProperties: UnionMetaProperties,
    private val metaLoaderProperties: UnionMetaLoaderProperties,
    private val kafkaGroupFactory: KafkaGroupFactory,
    private val kafkaConsumerFactory: RaribleKafkaConsumerFactory,
    private val meterRegistry: MeterRegistry,
    private val downloadTaskService: DownloadTaskService,
    private val ff: FeatureFlagsProperties,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val env = applicationEnvironmentInfo.name

    @Bean
    @Qualifier("item.meta.download.executor.manager")
    fun itemMetaDownloadExecutorManager(
        itemMetaRefreshService: ItemMetaRefreshService,
        enrichmentItemService: EnrichmentItemService,
        enrichmentBlacklistService: EnrichmentBlacklistService,
        itemMetaRepository: ItemMetaRepository,
        itemMetaDownloader: ItemMetaDownloader,
        partialItemMetaDownloader: PartialItemMetaDownloader,
        itemMetaNotifier: ItemMetaNotifier,
        itemDownloadExecutorMetrics: DownloadExecutorMetrics
    ): DownloadExecutorManager {
        val maxRetries = metaProperties.retryIntervals.size
        val executors = HashMap<String, DownloadExecutor<UnionMeta>>()
        ItemMetaPipeline.values().map { it.name.lowercase() }.forEach { pipeline ->
            val conf = getItemPipelineConfiguration(pipeline)
            val pool = DownloadPool(conf.poolSize, "item-meta-task-executor")
            val executor = ItemDownloadExecutor(
                itemMetaRefreshService,
                enrichmentItemService,
                enrichmentBlacklistService,
                itemMetaRepository,
                if (pipeline == ItemMetaPipeline.RETRY_PARTIAL.name.lowercase()) {
                    partialItemMetaDownloader
                } else {
                    itemMetaDownloader
                },
                itemMetaNotifier,
                pool,
                itemDownloadExecutorMetrics,
                maxRetries,
                metaLoaderProperties.downloader.limits,
                ff,
                metaProperties.simpleHash.enabled
            )
            executors[pipeline] = executor
            logger.info(
                "Created item-meta-task-executor (pipeline: $pipeline, poolSize: ${conf.poolSize})"
            )
        }
        return DownloadExecutorManager(executors)
    }

    @Bean
    @Deprecated("Replace with itemMetaDownloadTaskJob")
    fun eventItemMetaDownloadTaskConsumerFactory(): RaribleKafkaListenerContainerFactory<DownloadTaskEvent> =
        downloadTaskConsumerFactory(
            conf = getItemPipelineConfiguration(ItemMetaPipeline.EVENT.pipeline),
        )

    @Bean
    @Deprecated("Replace with itemMetaDownloadTaskJob")
    fun eventItemMetaDownloadTaskConsumer(
        @Qualifier("item.meta.download.executor.manager")
        executorManager: DownloadExecutorManager,
        eventItemMetaDownloadTaskConsumerFactory: RaribleKafkaListenerContainerFactory<DownloadTaskEvent>,
    ): ConcurrentMessageListenerContainer<String, DownloadTaskEvent> {
        val conf = getItemPipelineConfiguration(ItemMetaPipeline.EVENT.pipeline)
        val handler = DownloadExecutorHandler(ItemMetaPipeline.EVENT.pipeline, executorManager)
        return createDownloadExecutorConsumer(
            type = ITEM_TYPE,
            pipeline = ItemMetaPipeline.EVENT.pipeline,
            concurrency = conf.workers,
            handler = handler,
            factory = eventItemMetaDownloadTaskConsumerFactory,
        )
    }

    @Bean
    @Deprecated("Replace with itemMetaDownloadTaskJob")
    fun apiItemMetaDownloadTaskConsumerFactory(): RaribleKafkaListenerContainerFactory<DownloadTaskEvent> =
        downloadTaskConsumerFactory(
            conf = getItemPipelineConfiguration(ItemMetaPipeline.API.pipeline),
        )

    @Bean
    @Deprecated("Replace with itemMetaDownloadTaskJob")
    fun apiItemMetaDownloadTaskConsumer(
        @Qualifier("item.meta.download.executor.manager")
        executorManager: DownloadExecutorManager,
        apiItemMetaDownloadTaskConsumerFactory: RaribleKafkaListenerContainerFactory<DownloadTaskEvent>,
    ): ConcurrentMessageListenerContainer<String, DownloadTaskEvent> {
        val conf = getItemPipelineConfiguration(ItemMetaPipeline.API.pipeline)
        val handler = DownloadExecutorHandler(ItemMetaPipeline.API.pipeline, executorManager)
        return createDownloadExecutorConsumer(
            type = ITEM_TYPE,
            pipeline = ItemMetaPipeline.API.pipeline,
            concurrency = conf.workers,
            handler = handler,
            factory = apiItemMetaDownloadTaskConsumerFactory,
        )
    }

    @Bean
    @Deprecated("Replace with itemMetaDownloadTaskJob")
    fun refreshItemMetaDownloadTaskConsumerFactory(): RaribleKafkaListenerContainerFactory<DownloadTaskEvent> =
        downloadTaskConsumerFactory(
            conf = getItemPipelineConfiguration(ItemMetaPipeline.REFRESH.pipeline),
        )

    @Bean
    @Deprecated("Replace with itemMetaDownloadTaskJob")
    fun refreshItemMetaDownloadTaskConsumer(
        @Qualifier("item.meta.download.executor.manager")
        executorManager: DownloadExecutorManager,
        refreshItemMetaDownloadTaskConsumerFactory: RaribleKafkaListenerContainerFactory<DownloadTaskEvent>,
    ): ConcurrentMessageListenerContainer<String, DownloadTaskEvent> {
        val conf = getItemPipelineConfiguration(ItemMetaPipeline.REFRESH.pipeline)
        val handler = DownloadExecutorHandler(ItemMetaPipeline.REFRESH.pipeline, executorManager)
        return createDownloadExecutorConsumer(
            type = ITEM_TYPE,
            pipeline = ItemMetaPipeline.REFRESH.pipeline,
            concurrency = conf.workers,
            handler = handler,
            factory = refreshItemMetaDownloadTaskConsumerFactory,
        )
    }

    @Bean
    @Deprecated("Replace with itemMetaDownloadTaskJob")
    fun retryItemMetaDownloadTaskConsumerFactory(): RaribleKafkaListenerContainerFactory<DownloadTaskEvent> =
        downloadTaskConsumerFactory(
            conf = getItemPipelineConfiguration(ItemMetaPipeline.RETRY.pipeline),
        )

    @Bean
    @Deprecated("Replace with itemMetaDownloadTaskJob")
    fun retryItemMetaDownloadTaskConsumer(
        @Qualifier("item.meta.download.executor.manager")
        executorManager: DownloadExecutorManager,
        retryItemMetaDownloadTaskConsumerFactory: RaribleKafkaListenerContainerFactory<DownloadTaskEvent>,
    ): ConcurrentMessageListenerContainer<String, DownloadTaskEvent> {
        val conf = getItemPipelineConfiguration(ItemMetaPipeline.RETRY.pipeline)
        val handler = DownloadExecutorHandler(ItemMetaPipeline.RETRY.pipeline, executorManager)
        return createDownloadExecutorConsumer(
            type = ITEM_TYPE,
            pipeline = ItemMetaPipeline.RETRY.pipeline,
            concurrency = conf.workers,
            handler = handler,
            factory = retryItemMetaDownloadTaskConsumerFactory,
        )
    }

    @Bean
    @Deprecated("Replace with itemMetaDownloadTaskJob")
    fun retryPartialItemMetaDownloadTaskConsumerFactory(): RaribleKafkaListenerContainerFactory<DownloadTaskEvent> =
        downloadTaskConsumerFactory(
            conf = getItemPipelineConfiguration(ItemMetaPipeline.RETRY_PARTIAL.pipeline),
        )

    @Bean
    @Deprecated("Replace with itemMetaDownloadTaskJob")
    fun retryPartialItemMetaDownloadTaskConsumer(
        @Qualifier("item.meta.download.executor.manager")
        executorManager: DownloadExecutorManager,
        retryPartialItemMetaDownloadTaskConsumerFactory: RaribleKafkaListenerContainerFactory<DownloadTaskEvent>,
    ): ConcurrentMessageListenerContainer<String, DownloadTaskEvent> {
        val conf = getItemPipelineConfiguration(ItemMetaPipeline.RETRY_PARTIAL.pipeline)
        val handler = DownloadExecutorHandler(ItemMetaPipeline.RETRY_PARTIAL.pipeline, executorManager)
        return createDownloadExecutorConsumer(
            type = ITEM_TYPE,
            pipeline = ItemMetaPipeline.RETRY_PARTIAL.pipeline,
            concurrency = conf.workers,
            handler = handler,
            factory = retryPartialItemMetaDownloadTaskConsumerFactory,
        )
    }

    @Bean
    @Deprecated("Replace with itemMetaDownloadTaskJob")
    fun syncItemMetaDownloadTaskConsumerFactory(): RaribleKafkaListenerContainerFactory<DownloadTaskEvent> =
        downloadTaskConsumerFactory(
            conf = getItemPipelineConfiguration(ItemMetaPipeline.SYNC.pipeline),
        )

    @Bean
    @Deprecated("Replace with itemMetaDownloadTaskJob")
    fun syncItemMetaDownloadTaskConsumer(
        @Qualifier("item.meta.download.executor.manager")
        executorManager: DownloadExecutorManager,
        syncItemMetaDownloadTaskConsumerFactory: RaribleKafkaListenerContainerFactory<DownloadTaskEvent>,
    ): ConcurrentMessageListenerContainer<String, DownloadTaskEvent> {
        val conf = getItemPipelineConfiguration(ItemMetaPipeline.SYNC.pipeline)
        val handler = DownloadExecutorHandler(ItemMetaPipeline.SYNC.pipeline, executorManager)
        return createDownloadExecutorConsumer(
            type = ITEM_TYPE,
            pipeline = ItemMetaPipeline.SYNC.pipeline,
            concurrency = conf.workers,
            handler = handler,
            factory = syncItemMetaDownloadTaskConsumerFactory,
        )
    }

    private fun getItemPipelineConfiguration(pipeline: String): ExecutorPipelineProperties {
        val result = metaLoaderProperties.downloader.item[pipeline] ?: ExecutorPipelineProperties()
        logger.info("Settings for Item downloader pipeline '{}': {}", pipeline, result)
        return result
    }

    @Bean
    fun itemMetaDownloadTaskJob(
        @Qualifier("item.meta.download.executor.manager")
        executorManager: DownloadExecutorManager
    ): DownloadExecutorWorker {
        val jobs = ItemMetaPipeline.values().map { it.name.lowercase() }.map { pipeline ->
            createDownloadExecutorJob(pipeline, ITEM_TYPE, executorManager, getItemPipelineConfiguration(pipeline))
        }
        return DownloadExecutorWorker(ff.enableMetaMongoPipeline, jobs)
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
                maxRetries,
                ff,
                metaLoaderProperties.downloader.limits,
            )
            executors[pipeline] = executor
            logger.info(
                "Created collection-meta-task-executor (pipeline: $pipeline, poolSize: ${conf.poolSize})"
            )
        }
        return DownloadExecutorManager(executors)
    }

    @Bean
    @Deprecated("Replace with collectionMetaDownloadTaskJob")
    fun eventCollectionMetaDownloadTaskConsumerFactory(): RaribleKafkaListenerContainerFactory<DownloadTaskEvent> =
        downloadTaskConsumerFactory(
            conf = getCollectionPipelineConfiguration(CollectionMetaPipeline.EVENT.pipeline),
        )

    @Bean
    @Deprecated("Replace with collectionMetaDownloadTaskJob")
    fun eventCollectionMetaDownloadTaskConsumer(
        @Qualifier("collection.meta.download.executor.manager")
        executorManager: DownloadExecutorManager,
        eventCollectionMetaDownloadTaskConsumerFactory: RaribleKafkaListenerContainerFactory<DownloadTaskEvent>,
    ): ConcurrentMessageListenerContainer<String, DownloadTaskEvent> {
        val conf = getCollectionPipelineConfiguration(CollectionMetaPipeline.EVENT.pipeline)
        val handler = DownloadExecutorHandler(CollectionMetaPipeline.EVENT.pipeline, executorManager)
        return createDownloadExecutorConsumer(
            type = COLLECTION_TYPE,
            pipeline = CollectionMetaPipeline.EVENT.pipeline,
            concurrency = conf.workers,
            handler = handler,
            factory = eventCollectionMetaDownloadTaskConsumerFactory,
        )
    }

    @Bean
    @Deprecated("Replace with collectionMetaDownloadTaskJob")
    fun apiCollectionMetaDownloadTaskConsumerFactory(): RaribleKafkaListenerContainerFactory<DownloadTaskEvent> =
        downloadTaskConsumerFactory(
            conf = getCollectionPipelineConfiguration(CollectionMetaPipeline.API.pipeline),
        )

    @Bean
    @Deprecated("Replace with collectionMetaDownloadTaskJob")
    fun apiCollectionMetaDownloadTaskConsumer(
        @Qualifier("collection.meta.download.executor.manager")
        executorManager: DownloadExecutorManager,
        apiCollectionMetaDownloadTaskConsumerFactory: RaribleKafkaListenerContainerFactory<DownloadTaskEvent>,
    ): ConcurrentMessageListenerContainer<String, DownloadTaskEvent> {
        val conf = getCollectionPipelineConfiguration(CollectionMetaPipeline.API.pipeline)
        val handler = DownloadExecutorHandler(CollectionMetaPipeline.API.pipeline, executorManager)
        return createDownloadExecutorConsumer(
            type = COLLECTION_TYPE,
            pipeline = CollectionMetaPipeline.API.pipeline,
            concurrency = conf.workers,
            handler = handler,
            factory = apiCollectionMetaDownloadTaskConsumerFactory,
        )
    }

    @Bean
    @Deprecated("Replace with collectionMetaDownloadTaskJob")
    fun refreshCollectionMetaDownloadTaskConsumerFactory(): RaribleKafkaListenerContainerFactory<DownloadTaskEvent> =
        downloadTaskConsumerFactory(
            conf = getCollectionPipelineConfiguration(CollectionMetaPipeline.REFRESH.pipeline),
        )

    @Bean
    @Deprecated("Replace with collectionMetaDownloadTaskJob")
    fun refreshCollectionMetaDownloadTaskConsumer(
        @Qualifier("collection.meta.download.executor.manager")
        executorManager: DownloadExecutorManager,
        refreshCollectionMetaDownloadTaskConsumerFactory: RaribleKafkaListenerContainerFactory<DownloadTaskEvent>,
    ): ConcurrentMessageListenerContainer<String, DownloadTaskEvent> {
        val conf = getCollectionPipelineConfiguration(CollectionMetaPipeline.REFRESH.pipeline)
        val handler = DownloadExecutorHandler(CollectionMetaPipeline.REFRESH.pipeline, executorManager)
        return createDownloadExecutorConsumer(
            type = COLLECTION_TYPE,
            pipeline = CollectionMetaPipeline.REFRESH.pipeline,
            concurrency = conf.workers,
            handler = handler,
            factory = refreshCollectionMetaDownloadTaskConsumerFactory,
        )
    }

    @Bean
    @Deprecated("Replace with collectionMetaDownloadTaskJob")
    fun retryCollectionMetaDownloadTaskConsumerFactory(): RaribleKafkaListenerContainerFactory<DownloadTaskEvent> =
        downloadTaskConsumerFactory(
            conf = getCollectionPipelineConfiguration(CollectionMetaPipeline.RETRY.pipeline),
        )

    @Bean
    @Deprecated("Replace with collectionMetaDownloadTaskJob")
    fun retryCollectionMetaDownloadTaskConsumer(
        @Qualifier("collection.meta.download.executor.manager")
        executorManager: DownloadExecutorManager,
        retryCollectionMetaDownloadTaskConsumerFactory: RaribleKafkaListenerContainerFactory<DownloadTaskEvent>,
    ): ConcurrentMessageListenerContainer<String, DownloadTaskEvent> {
        val conf = getCollectionPipelineConfiguration(CollectionMetaPipeline.RETRY.pipeline)
        val handler = DownloadExecutorHandler(CollectionMetaPipeline.RETRY.pipeline, executorManager)
        return createDownloadExecutorConsumer(
            type = COLLECTION_TYPE,
            pipeline = CollectionMetaPipeline.RETRY.pipeline,
            concurrency = conf.workers,
            handler = handler,
            factory = retryCollectionMetaDownloadTaskConsumerFactory,
        )
    }

    @Bean
    @Deprecated("Replace with collectionMetaDownloadTaskJob")
    fun syncCollectionMetaDownloadTaskConsumerFactory(): RaribleKafkaListenerContainerFactory<DownloadTaskEvent> =
        downloadTaskConsumerFactory(
            conf = getCollectionPipelineConfiguration(CollectionMetaPipeline.SYNC.pipeline),
        )

    @Bean
    @Deprecated("Replace with collectionMetaDownloadTaskJob")
    fun syncCollectionMetaDownloadTaskConsumer(
        @Qualifier("collection.meta.download.executor.manager")
        executorManager: DownloadExecutorManager,
        syncCollectionMetaDownloadTaskConsumerFactory: RaribleKafkaListenerContainerFactory<DownloadTaskEvent>,
    ): ConcurrentMessageListenerContainer<String, DownloadTaskEvent> {
        val conf = getCollectionPipelineConfiguration(CollectionMetaPipeline.SYNC.pipeline)
        val handler = DownloadExecutorHandler(CollectionMetaPipeline.SYNC.pipeline, executorManager)
        return createDownloadExecutorConsumer(
            type = COLLECTION_TYPE,
            pipeline = CollectionMetaPipeline.SYNC.pipeline,
            concurrency = conf.workers,
            handler = handler,
            factory = syncCollectionMetaDownloadTaskConsumerFactory,
        )
    }

    @Bean
    fun collectionMetaDownloadTaskJob(
        @Qualifier("collection.meta.download.executor.manager")
        executorManager: DownloadExecutorManager
    ): DownloadExecutorWorker {
        val jobs = CollectionMetaPipeline.values().map { it.name.lowercase() }.map { pipeline ->
            createDownloadExecutorJob(
                pipeline,
                COLLECTION_TYPE,
                executorManager,
                getCollectionPipelineConfiguration(pipeline)
            )
        }
        return DownloadExecutorWorker(ff.enableMetaMongoPipeline, jobs)
    }

    private fun downloadTaskConsumerFactory(
        conf: ExecutorPipelineProperties,
    ): RaribleKafkaListenerContainerFactory<DownloadTaskEvent> {
        logger.info(
            "Created downloadTaskConsumerFactory consumers for (batchSize: ${conf.batchSize})"
        )
        return RaribleKafkaListenerContainerFactory(
            settings = RaribleKafkaContainerFactorySettings(
                hosts = metaLoaderProperties.brokerReplicaSet,
                valueClass = DownloadTaskEvent::class.java,
                concurrency = conf.workers,
                batchSize = conf.batchSize,
                deserializer = UnionKafkaJsonDeserializer::class.java,
            )
        )
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
        handler: RaribleKafkaBatchEventHandler<DownloadTaskEvent>,
        factory: RaribleKafkaListenerContainerFactory<DownloadTaskEvent>,
    ): ConcurrentMessageListenerContainer<String, DownloadTaskEvent> {
        val topic = when (type) {
            COLLECTION_TYPE -> UnionInternalTopicProvider.getCollectionMetaDownloadTaskExecutorTopic(env, pipeline)
            ITEM_TYPE -> UnionInternalTopicProvider.getItemMetaDownloadTaskExecutorTopic(env, pipeline)
            else -> throw IllegalArgumentException("Unsupported type for meta-loader: $type")
        }
        val settings = RaribleKafkaConsumerSettings(
            topic = topic,
            group = kafkaGroupFactory.metaDownloadExecutorGroup(type),
            async = false,
        )
        logger.info(
            "Created $concurrency consumers for $type-download-executor (pipeline: $pipeline)"
        )
        val container = kafkaConsumerFactory.createWorker(settings, handler, factory)
        container.concurrency = concurrency
        return container
    }

    private fun createDownloadExecutorJob(
        pipeline: String,
        type: String,
        executorManager: DownloadExecutorManager,
        conf: ExecutorPipelineProperties
    ): DownloadExecutorJob {
        logger.info(
            "Created job for $type-download-executor (pipeline: $pipeline, poolSize: ${conf.poolSize})"
        )
        return DownloadExecutorJob(
            meterRegistry = meterRegistry,
            workerName = "meta_${type}_downloader_$pipeline",
            downloadTaskService = downloadTaskService,
            executor = executorManager.getExecutor(pipeline),
            pipeline = pipeline,
            poolSize = conf.poolSize
        )
    }
}
