package com.rarible.protocol.union.worker.job.meta

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.job.JobHandler
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaService
import com.rarible.protocol.union.enrichment.model.ItemMetaCustomAttributes
import com.rarible.protocol.union.enrichment.model.MetaDownloadPriority
import com.rarible.protocol.union.enrichment.repository.ItemMetaCustomAttributesRepository
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.time.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ItemMetaCustomAttributesJob(
    private val handler: ItemMetaCustomAttributesJobHandler,
    properties: WorkerProperties,
    meterRegistry: MeterRegistry,
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.itemMetaCustomAttributesJob.rate,
        errorDelay = Duration.ofMinutes(5)
    ),
    workerName = "item-meta-custom-attributes-job"
) {

    override suspend fun handle() {
        handler.handle()
        delay(pollingPeriod)
    }
}

@Component
class ItemMetaCustomAttributesJobHandler(
    private val repository: ItemMetaCustomAttributesRepository,
    private val providers: List<MetaCustomAttributesProvider>,
    private val itemMetaService: ItemMetaService
) : JobHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle() {
        providers.forEach {
            try {
                handle(it)
            } catch (e: Exception) {
                logger.error("Failed to update Item meta custom attributes for {}", it.name, e)
            }
        }
    }

    private suspend fun handle(provider: MetaCustomAttributesProvider) {
        logger.info("Starting to fetch Item meta custom attributes for {}", provider.name)
        val attributes = provider.getCustomAttributes()
        logger.info("Found {} Item meta custom attributes for {}", attributes.size, provider.name)

        var changed = 0

        attributes.chunked(1000).forEach { chunk ->
            val exists = repository.getAll(chunk.map { it.id }).associateBy { it.id }
            chunk.forEach { attributes ->
                val actual = ItemMetaCustomAttributes(
                    id = attributes.id.fullId(),
                    attributes = attributes.attributes.sortedBy { it.key }
                )
                if (actual != exists[actual.id]) {
                    repository.save(actual)
                    itemMetaService.schedule(
                        itemId = attributes.id,
                        pipeline = ItemMetaPipeline.REFRESH,
                        force = true,
                        priority = MetaDownloadPriority.HIGH
                    )
                    changed++
                }
            }
        }

        logger.info("Updated {} of {} Item custom meta attributes for {}", changed, attributes.size, provider.name)
    }
}
