package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.converter.EsItemConverter.toEsItem
import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDeleteEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.search.indexer.metrics.IndexerMetricFactory
import org.elasticsearch.action.support.WriteRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ItemEventHandler(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val repository: EsItemRepository,
    metricFactory: IndexerMetricFactory,
) : ConsumerBatchEventHandler<ItemEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val entitySaveCounters = BlockchainDto.values().associateWith {
        metricFactory.createEntitySaveCountMetric(EsEntity.ITEM, it)
    }

    override suspend fun handle(event: List<ItemEventDto>) {
        logger.info("Handling ${event.size} ItemEventDto events")
        val startTime = nowMillis()

        val convertedEvents = event
            .filterIsInstance<ItemUpdateEventDto>()
            .map {
                it as ItemUpdateEventDto
                logger.debug("Converting ItemDto id = ${it.itemId.fullId()}")
                it.item.toEsItem()
            }
        logger.debug("Saving ${convertedEvents.size} ItemUpdateEventDto events to ElasticSearch")
        val refreshPolicy =
            if (featureFlagsProperties.enableItemSaveImmediateToElasticSearch) {
                WriteRequest.RefreshPolicy.IMMEDIATE
            } else {
                if (convertedEvents.any { it.self == true })
                    WriteRequest.RefreshPolicy.IMMEDIATE
                else
                    WriteRequest.RefreshPolicy.NONE
            }

        val deletedIds = event
            .filterIsInstance<ItemDeleteEventDto>()
            .map {
                it.itemId.fullId()
            }

        repository.bulk(convertedEvents, deletedIds, refreshPolicy = refreshPolicy)
        countSaves(convertedEvents)

        val elapsedTime = nowMillis().minusMillis(startTime.toEpochMilli()).toEpochMilli()
        logger.info("Handling of ${event.size} ItemEventDto events completed in $elapsedTime ms" +
                " (saved: ${convertedEvents.size}, deleted: ${deletedIds.size})")
    }

    private fun countSaves(items: List<EsItem>) {
        val countByBlockchain = items.groupingBy { it.blockchain }.eachCount()
        countByBlockchain.entries.forEach {
            entitySaveCounters[it.key]!!.increment(it.value.toDouble())
        }
    }
}
