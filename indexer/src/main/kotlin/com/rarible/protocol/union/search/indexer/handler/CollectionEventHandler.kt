package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.converter.EsCollectionConverter
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.search.indexer.metrics.IndexerMetricFactory
import org.elasticsearch.action.support.WriteRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CollectionEventHandler(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val repository: EsCollectionRepository,
    metricFactory: IndexerMetricFactory,
) : ConsumerBatchEventHandler<CollectionEventDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val entitySaveCounters = BlockchainDto.values().associateWith {
        metricFactory.createEntitySaveCountMetric(EsEntity.COLLECTION, it)
    }

    override suspend fun handle(event: List<CollectionEventDto>) {
        logger.info("Handling ${event.size} CollectionEventDto events")
        val startTime = nowMillis()

        val collectionsToSave = mutableListOf<EsCollection>()
        val idsToDelete = mutableListOf<String>()

        event.forEach {
            it as CollectionUpdateEventDto
            if (it.collection.status == CollectionDto.Status.ERROR) {
                idsToDelete.add(it.collection.id.fullId())
            } else {
                collectionsToSave.add(EsCollectionConverter.convert(it.collection))
            }
        }

        val refreshPolicy = if (featureFlagsProperties.enableCollectionSaveImmediateToElasticSearch) {
            WriteRequest.RefreshPolicy.IMMEDIATE
        } else {
            if (collectionsToSave.any { it.self == true })
                WriteRequest.RefreshPolicy.IMMEDIATE
            else
                WriteRequest.RefreshPolicy.NONE
        }

        repository.bulk(collectionsToSave, idsToDelete, refreshPolicy = refreshPolicy)
        countSaves(collectionsToSave)
        val elapsedTime = nowMillis().minusMillis(startTime.toEpochMilli()).toEpochMilli()
        logger.info(
            "Handling of ${event.size} CollectionEventDto events completed in $elapsedTime ms" +
                    " (saved ${collectionsToSave.size}, deleted ${idsToDelete.size})"
        )
    }

    private fun countSaves(collections: List<EsCollection>) {
        val countByBlockchain = collections.groupingBy { it.blockchain }.eachCount()
        countByBlockchain.entries.forEach {
            entitySaveCounters[it.key]!!.increment(it.value.toDouble())
        }
    }
}
