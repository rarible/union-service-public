package com.rarible.protocol.union.listener.job

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.job.JobHandler
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.ItemMetaService
import com.rarible.protocol.union.listener.config.UnionListenerProperties
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.time.delay
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["common.feature-flags.enableMetaPipeline"], havingValue = "true")
class ItemMetaRetryJob(
    private val handler: ItemMetaRetryJobHandler,
    listenerProperties: UnionListenerProperties,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = listenerProperties.metaItemRetry.rate,
        errorDelay = listenerProperties.metaItemRetry.rate
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
    private val metaService: ItemMetaService
) : JobHandler {
    override suspend fun handle() {
        val now = nowMillis()
        val retryIntervals = metaProperties.retryIntervals

        for (i in retryIntervals.indices) {
            val itemFlow = repository.getItemForMetaRetry(now, retryIntervals[i], i)

            itemFlow.collect {
                metaService.schedule(it.id.toDto(), ItemMetaPipeline.RETRY, true)
                repository.save(it.withNextRetry())
            }
        }
    }
}
