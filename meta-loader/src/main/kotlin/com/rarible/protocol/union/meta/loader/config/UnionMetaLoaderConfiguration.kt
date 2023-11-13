package com.rarible.protocol.union.meta.loader.config

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.enrichment.configuration.CommonMetaProperties
import com.rarible.protocol.union.enrichment.configuration.EnrichmentApiConfiguration
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
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
import com.rarible.protocol.union.meta.loader.executor.DownloadExecutorMetrics
import com.rarible.protocol.union.meta.loader.executor.DownloadPool
import com.rarible.protocol.union.meta.loader.executor.ItemDownloadExecutor
import com.rarible.protocol.union.meta.loader.job.DownloadExecutorJob
import com.rarible.protocol.union.meta.loader.job.DownloadExecutorWorker
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(value = [EnrichmentApiConfiguration::class, SearchConfiguration::class])
@EnableConfigurationProperties(value = [UnionMetaLoaderProperties::class])
class UnionMetaLoaderConfiguration(
    private val metaProperties: CommonMetaProperties,
    private val metaLoaderProperties: UnionMetaLoaderProperties,
    private val meterRegistry: MeterRegistry,
    private val downloadTaskService: DownloadTaskService,
    private val ff: FeatureFlagsProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun itemMetaDownloadTaskJob(
        itemMetaRefreshService: ItemMetaRefreshService,
        enrichmentItemService: EnrichmentItemService,
        enrichmentBlacklistService: EnrichmentBlacklistService,
        itemMetaRepository: ItemMetaRepository,
        itemMetaDownloader: ItemMetaDownloader,
        partialItemMetaDownloader: PartialItemMetaDownloader,
        itemMetaNotifier: ItemMetaNotifier,
        itemDownloadExecutorMetrics: DownloadExecutorMetrics,
    ): DownloadExecutorWorker {
        val jobs = ItemMetaPipeline.values().map { pipeline ->
            val conf = metaLoaderProperties.downloader.item[pipeline.pipeline] ?: ExecutorPipelineProperties()
            val downloader = when (pipeline) {
                ItemMetaPipeline.RETRY_PARTIAL -> partialItemMetaDownloader
                else -> itemMetaDownloader
            }
            val executor = ItemDownloadExecutor(
                itemMetaRefreshService = itemMetaRefreshService,
                enrichmentItemService = enrichmentItemService,
                enrichmentBlacklistService = enrichmentBlacklistService,
                repository = itemMetaRepository,
                downloader = downloader,
                notifier = itemMetaNotifier,
                pool = DownloadPool(conf.poolSize, "item-meta-task-executor"),
                metrics = itemDownloadExecutorMetrics,
                maxRetries = metaProperties.retryIntervals.size,
                limits = metaLoaderProperties.downloader.limits,
                simpleHashEnabled = metaProperties.simpleHash.enabled,
                ff = ff,
            )
            createDownloadExecutorJob(
                pipeline = pipeline.pipeline,
                executor = executor,
                conf = conf
            )
        }
        return DownloadExecutorWorker(workers = jobs)
    }

    @Bean
    fun collectionMetaDownloadTaskJob(
        enrichmentBlacklistService: EnrichmentBlacklistService,
        collectionMetaRepository: CollectionMetaRepository,
        collectionMetaDownloader: CollectionMetaDownloader,
        collectionMetaNotifier: CollectionMetaNotifier,
        collectionDownloadExecutorMetrics: DownloadExecutorMetrics
    ): DownloadExecutorWorker {
        val jobs = CollectionMetaPipeline.values().map { pipeline ->
            val conf = metaLoaderProperties.downloader.item[pipeline.pipeline] ?: ExecutorPipelineProperties()
            val executor = CollectionDownloadExecutor(
                enrichmentBlacklistService = enrichmentBlacklistService,
                repository = collectionMetaRepository,
                downloader = collectionMetaDownloader,
                notifier = collectionMetaNotifier,
                pool = DownloadPool(conf.poolSize, "collection-meta-task-executor"),
                metrics = collectionDownloadExecutorMetrics,
                maxRetries = metaProperties.retryIntervals.size,
                limits = metaLoaderProperties.downloader.limits,
                ff = ff,
            )
            createDownloadExecutorJob(
                pipeline = pipeline.pipeline,
                executor = executor,
                conf = conf
            )
        }
        return DownloadExecutorWorker(workers = jobs)
    }

    private fun createDownloadExecutorJob(
        pipeline: String,
        executor: DownloadExecutor<*>,
        conf: ExecutorPipelineProperties
    ): DownloadExecutorJob {
        logger.info("Created job for ${executor.type} DownloadExecutorJob for pipeline '$pipeline': $conf)")
        return DownloadExecutorJob(
            meterRegistry = meterRegistry,
            workerName = "meta_${executor.type}_downloader_$pipeline",
            downloadTaskService = downloadTaskService,
            executor = executor,
            pipeline = pipeline,
            poolSize = conf.poolSize
        )
    }
}
