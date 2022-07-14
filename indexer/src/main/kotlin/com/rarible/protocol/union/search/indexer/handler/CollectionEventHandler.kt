package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.converter.EsCollectionConverter
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.search.indexer.metrics.IndexerMetricFactory
import org.elasticsearch.action.support.WriteRequest
import org.springframework.stereotype.Service

@Service
class CollectionEventHandler(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val repository: EsCollectionRepository,
    metricFactory: IndexerMetricFactory,
) : ConsumerBatchEventHandler<CollectionEventDto> {

    companion object {
        private val logger by Logger()
    }

    private val entitySaveCounters = BlockchainDto.values().associateWith {
        metricFactory.createEntitySaveCountMetric(EsEntity.COLLECTION, it)
    }

    override suspend fun handle(event: List<CollectionEventDto>) {
        logger.info("Handling ${event.size} CollectionEventDto events")
        val startTime = nowMillis()

        val convertedEvents = event.map {
            it as CollectionUpdateEventDto
            logger.debug("Converting CollectionDto id = ${it.collection.id.fullId()}")
            EsCollectionConverter.convert(it.collection)
        }
        logger.debug("Saving ${convertedEvents.size} CollectionEventDto events to ElasticSearch")
        val refreshPolicy =
            if (featureFlagsProperties.enableItemSaveImmediateToElasticSearch) {
                WriteRequest.RefreshPolicy.IMMEDIATE
            } else {
                if (convertedEvents.any { it.self == true })
                    WriteRequest.RefreshPolicy.IMMEDIATE
                else
                    WriteRequest.RefreshPolicy.NONE
            }

        repository.saveAll(convertedEvents, refreshPolicy = refreshPolicy)
        countSaves(convertedEvents)
        val elapsedTime = nowMillis().minusMillis(startTime.toEpochMilli()).toEpochMilli()
        logger.info("Handling of ${event.size} CollectionEventDto events completed in $elapsedTime ms")
    }

    private fun countSaves(collections: List<EsCollection>) {
        val countByBlockchain = collections.groupingBy { it.blockchain }.eachCount()
        countByBlockchain.entries.forEach {
            entitySaveCounters[it.key]!!.increment(it.value.toDouble())
        }
    }
}
