package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.converter.EsTraitConverter.toEsTrait
import com.rarible.protocol.union.core.model.UnionTraitEvent
import com.rarible.protocol.union.core.model.elastic.EsEntity
import com.rarible.protocol.union.core.model.elastic.EsTrait
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.search.EsTraitRepository
import com.rarible.protocol.union.search.indexer.metrics.IndexerMetricFactory
import org.elasticsearch.action.support.WriteRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TraitEventHandler(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val repository: EsTraitRepository,
    metricFactory: IndexerMetricFactory,
) : RaribleKafkaBatchEventHandler<UnionTraitEvent> {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val entitySaveCounters = BlockchainDto.values().associateWith {
        metricFactory.createEntitySaveCountMetric(EsEntity.TRAIT, it)
    }

    override suspend fun handle(event: List<UnionTraitEvent>) {
        logger.info("Handling ${event.size} UnionTraitEvent events")
        val startTime = nowMillis()

        val convertedEvents = event
            .filter { it.itemsCount > 0 }
            .map {
                it.toEsTrait()
            }
        logger.debug("Saving ${convertedEvents.size} UnionTraitEvent events to ElasticSearch")
        val refreshPolicy =
            if (featureFlagsProperties.enableTraitSaveImmediateToElasticSearch) {
                WriteRequest.RefreshPolicy.IMMEDIATE
            } else {
                WriteRequest.RefreshPolicy.NONE
            }

        val deletedIds = event
            .filter { it.itemsCount <= 0 }
            .map {
                it.id
            }

        repository.bulk(convertedEvents, deletedIds, refreshPolicy = refreshPolicy)
        countSaves(convertedEvents)

        val elapsedTime = nowMillis().minusMillis(startTime.toEpochMilli()).toEpochMilli()
        logger.info("Handling of ${event.size} UnionTraitEvent events completed in $elapsedTime ms" +
                " (saved: ${convertedEvents.size}, deleted: ${deletedIds.size})")
    }

    private fun countSaves(traits: List<EsTrait>) {
        val countByBlockchain = traits.groupingBy { it.blockchain }.eachCount()
        countByBlockchain.entries.forEach {
            entitySaveCounters[it.key]!!.increment(it.value.toDouble())
        }
    }
}
