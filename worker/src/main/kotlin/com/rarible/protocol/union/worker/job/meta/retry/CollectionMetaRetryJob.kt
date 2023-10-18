package com.rarible.protocol.union.worker.job.meta.retry

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.job.JobHandler
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.enrichment.configuration.CommonMetaProperties
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaService
import com.rarible.protocol.union.enrichment.model.MetaDownloadPriority
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CollectionMetaRetryJob(
    private val handler: CollectionMetaRetryJobHandler,
    properties: WorkerProperties,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.metaCollectionRetry.rate,
        errorDelay = properties.metaCollectionRetry.rate
    ),
    workerName = "collection-meta-retry-job"
) {

    override suspend fun handle() {
        handler.handle()
        delay(pollingPeriod)
    }
}

@Component
class CollectionMetaRetryJobHandler(
    private val repository: CollectionRepository,
    private val metaProperties: CommonMetaProperties,
    private val metaService: CollectionMetaService
) : JobHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle() {
        val now = nowMillis()
        val retryIntervals = metaProperties.retryIntervals

        for (attempt in retryIntervals.indices) {
            val collectionFlow = repository.getCollectionsForMetaRetry(now, retryIntervals[attempt], attempt)

            collectionFlow.collect { collection ->
                val collectionId = collection.id.toDto()

                LogUtils.addToMdc(collectionId) {
                    logger.info("Scheduling Collection meta download (retry = $attempt) for $collectionId")
                }

                val priority = when (attempt) {
                    0 -> MetaDownloadPriority.ASAP
                    1 -> MetaDownloadPriority.HIGH
                    2 -> MetaDownloadPriority.MEDIUM
                    3 -> MetaDownloadPriority.LOW
                    else -> MetaDownloadPriority.NOBODY_CARES
                }

                metaService.schedule(
                    collectionId = collectionId,
                    pipeline = CollectionMetaPipeline.RETRY,
                    force = true,
                    priority = priority
                )
                repository.save(collection.withNextRetry())
            }
        }
    }
}
