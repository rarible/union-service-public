package com.rarible.protocol.union.listener.job.task

import com.rarible.core.logging.Logger
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.enrichment.model.CollectionStatistics
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.listener.clickhouse.repository.ClickHouseCollectionStatisticsRepository
import com.rarible.protocol.union.listener.service.EnrichmentCollectionEventService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class CollectionStatisticsResyncTask(
    private val clickHouseCollectionStatisticsRepository: ClickHouseCollectionStatisticsRepository,
    private val enrichmentCollectionEventService: EnrichmentCollectionEventService
) : TaskHandler<String> {

    override val type: String = TYPE

    override fun runLongTask(from: String?, param: String): Flow<String> = flow {
        logger.info("CollectionStatisticsResyncTask started with from={} and param={}", from, param)
        val counter = AtomicLong()
        val limit = param.toInt()
        var statisticsMap: Map<ShortCollectionId, CollectionStatistics>
        do {
            statisticsMap = clickHouseCollectionStatisticsRepository.getAllStatistics(
                fromIdExcluded = from,
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
                emit(it.toString())
            }
        } while (statisticsMap.size >= limit)
        logger.info("CollectionStatisticsResyncTask with from={} and param={} ended", from, param)
    }

    companion object {
        private val logger by Logger()
        const val TYPE = "COLLECTION_STATISTICS_RESYNC_TASK"
    }
}
