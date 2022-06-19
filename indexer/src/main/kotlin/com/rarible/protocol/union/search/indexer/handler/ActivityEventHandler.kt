package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.search.indexer.metrics.IndexerMetricFactory
import org.springframework.stereotype.Service

@Service
class ActivityEventHandler(
    private val repository: EsActivityRepository,
    private val router: BlockchainRouter<ItemService>,
    metricFactory: IndexerMetricFactory,
): ConsumerBatchEventHandler<ActivityDto> {

    companion object {
        private val logger by Logger()
    }

    private val entitySaveCounters = BlockchainDto.values().associateWith {
        metricFactory.createEntitySaveCountMetric(EsEntity.ACTIVITY, it)
    }

    override suspend fun handle(event: List<ActivityDto>) {
        logger.info("Handling ${event.size} ActivityDto events")
        val startTime = nowMillis()

        val normalEvents = mutableListOf<ActivityDto>()
        val revertedEvents = mutableListOf<ActivityDto>()

        event.forEach {
            if (it.reverted == true) {
                revertedEvents.add(it)
            } else {
                normalEvents.add(it)
            }
        }

        val convertedEvents = EsActivityConverter.batchConvert(normalEvents, router)

        if (convertedEvents.isNotEmpty()) {
            repository.saveAll(convertedEvents)
            countSaves(convertedEvents)
            logger.info("Saved ${convertedEvents.size} activities")
        }
        if (revertedEvents.isNotEmpty()) {
            val deleted = repository.delete(revertedEvents.map { it.id.toString() })
            logger.info("Deleted $deleted activities")
        }
        val elapsedTime = nowMillis().minusMillis(startTime.toEpochMilli()).toEpochMilli()
        logger.info("Handling of ${event.size} ActivityDto events completed in $elapsedTime ms")
    }

    private fun countSaves(activities: List<EsActivity>) {
        val countByBlockchain = activities.groupingBy { it.blockchain }.eachCount()
        countByBlockchain.entries.forEach {
            entitySaveCounters[it.key]!!.increment(it.value.toDouble())
        }
    }
}
