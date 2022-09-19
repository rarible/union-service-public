package com.rarible.protocol.union.listener.job.task

import com.rarible.core.logging.Logger
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.enrichment.model.CollectionStatistics
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.listener.clickhouse.configuration.ConditionalOnClickhouseEnabled
import com.rarible.protocol.union.listener.clickhouse.repository.ClickHouseCollectionStatisticsRepository
import com.rarible.protocol.union.listener.service.EnrichmentCollectionEventService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

@Component
@ConditionalOnClickhouseEnabled
class CollectionStatisticsResyncTask(
    private val clickHouseCollectionStatisticsRepository: ClickHouseCollectionStatisticsRepository,
    private val enrichmentCollectionEventService: EnrichmentCollectionEventService
) : TaskHandler<String> {

    override val type: String = TYPE

    override fun runLongTask(from: String?, param: String): Flow<String> = flow {
        logger.info("CollectionStatisticsResyncTask started with from={} and param={}", from, param)
        val counter = AtomicLong()
        val limit = param.toInt()
        val lastId = AtomicReference(from)
        do {
            val statisticsMap: Map<ShortCollectionId, CollectionStatistics> =
                clickHouseCollectionStatisticsRepository.getAllStatistics(
                    fromIdExcluded = lastId.get(),
                    limit = limit
                )

            statisticsMap.forEach { (collectionId, statistics) ->
                enrichmentCollectionEventService.onCollectionStatisticsUpdate(
                    collectionId, statistics, notificationEnabled = true
                )
            }

            statisticsMap.keys.lastOrNull()?.let {
                logger.info(
                    "Processed {} collections  with from={} and param={}",
                    counter.addAndGet(statisticsMap.size.toLong()),
                    from,
                    param
                )
                val last = it.toString()
                lastId.set(last)
                emit(last)
            }
        } while (statisticsMap.size >= limit)
        logger.info("CollectionStatisticsResyncTask with from={} and param={} ended", from, param)
    }

    companion object {
        private val logger by Logger()
        const val TYPE = "COLLECTION_STATISTICS_RESYNC_TASK"
    }
}
