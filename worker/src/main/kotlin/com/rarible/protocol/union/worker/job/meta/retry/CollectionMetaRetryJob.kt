package com.rarible.protocol.union.worker.job.meta.retry

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.job.JobHandler
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaService
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
    private val metaProperties: UnionMetaProperties,
    private val metaService: CollectionMetaService
) : JobHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle() {
        val now = nowMillis()
        val retryIntervals = metaProperties.retryIntervals

        for (i in retryIntervals.indices) {
            val collectionFlow = repository.getCollectionsForMetaRetry(now, retryIntervals[i], i)

            collectionFlow.collect { collection ->
                val collectionId = collection.id.toDto()

                LogUtils.addToMdc(collectionId) {
                    logger.info("Scheduling Collection meta download (retry = $i) for $collectionId")
                }

                metaService.schedule(collectionId, CollectionMetaPipeline.RETRY, true)
                repository.save(collection.withNextRetry())
            }
        }
    }
}
