package com.rarible.protocol.union.worker.job.meta.retry

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.job.JobHandler
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaService
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemMetaRetryJob(
    private val handler: ItemMetaRetryJobHandler,
    properties: WorkerProperties,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.metaItemRetry.rate,
        errorDelay = properties.metaItemRetry.rate
    ),
    workerName = "item-meta-retry-job"
) {
    override suspend fun handle() {
        handler.handle()
        delay(pollingPeriod)
    }
}

@Component
class ItemMetaRetryJobHandler(
    private val repository: ItemRepository,
    private val metaProperties: UnionMetaProperties,
    private val metaService: ItemMetaService,
    private val router: BlockchainRouter<ItemService>
) : JobHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle() {
        val now = nowMillis()
        val retryIntervals = metaProperties.retryIntervals

        for (i in retryIntervals.indices) {
            val itemFlow = repository.getItemForMetaRetry(now, retryIntervals[i], i)

            itemFlow.collect { item ->
                val itemId = item.id.toDto()

                LogUtils.addToMdc(
                    itemId,
                    item.collectionId?.let { CollectionIdDto(item.blockchain, it) },
                    router
                ) {
                    logger.info("Scheduling item meta download (retry = $i) for $itemId")
                }

                metaService.schedule(itemId, ItemMetaPipeline.RETRY, true)
                repository.save(item.withNextRetry())
            }
        }
    }
}
