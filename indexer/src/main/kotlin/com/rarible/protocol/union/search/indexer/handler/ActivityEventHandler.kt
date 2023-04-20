package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.search.indexer.metrics.IndexerMetricFactory
import org.elasticsearch.action.support.WriteRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ActivityEventHandler(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val repository: EsActivityRepository,
    private val converter: EsActivityConverter,
    metricFactory: IndexerMetricFactory,
) : ConsumerBatchEventHandler<ActivityDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val entitySaveCounters = BlockchainDto.values().associateWith {
        metricFactory.createEntitySaveCountMetric(EsEntity.ACTIVITY, it)
    }

    override suspend fun handle(event: List<ActivityDto>) {
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

        val convertedEvents = converter.batchConvert(normalEvents)

        val refreshPolicy =
            if (featureFlagsProperties.enableActivitySaveImmediateToElasticSearch) {
                WriteRequest.RefreshPolicy.IMMEDIATE
            } else {
                WriteRequest.RefreshPolicy.NONE
            }

        val idsToDelete = revertedEvents.map { it.id.toString() }
        repository.bulk(convertedEvents, idsToDelete, refreshPolicy = refreshPolicy)
        countSaves(convertedEvents)

        val elapsedTime = nowMillis().minusMillis(startTime.toEpochMilli()).toEpochMilli()
        logger.info(
            "Handling of ${event.size} ActivityDto events completed in $elapsedTime ms" +
                " (saved ${convertedEvents.size}, deleted ${revertedEvents.size})"
        )
    }

    private fun countSaves(activities: List<EsActivity>) {
        val countByBlockchain = activities.groupingBy { it.blockchain }.eachCount()
        countByBlockchain.entries.forEach {
            entitySaveCounters[it.key]!!.increment(it.value.toDouble())
        }
    }
}
