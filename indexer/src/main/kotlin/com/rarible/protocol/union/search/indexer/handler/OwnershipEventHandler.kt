package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.converter.EsOwnershipConverter
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.search.indexer.metrics.IndexerMetricFactory
import org.elasticsearch.action.support.WriteRequest
import org.springframework.stereotype.Service

@Service
class OwnershipEventHandler(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val repository: EsOwnershipRepository,
    metricFactory: IndexerMetricFactory,
) : ConsumerBatchEventHandler<OwnershipEventDto> {

    private val logger by Logger()

    private val entitySaveCounters = BlockchainDto.values().associateWith {
        metricFactory.createEntitySaveCountMetric(EsEntity.OWNERSHIP, it)
    }

    override suspend fun handle(event: List<OwnershipEventDto>) {
        logger.info("Handling ${event.size} OwnershipDto events")
        val startTime = nowMillis()

        val events = event.filterIsInstance<OwnershipUpdateEventDto>().map {
            logger.debug("Converting OwnershipDto id = ${it.ownershipId}")
            EsOwnershipConverter.convert(it.ownership)
        }

        val deleted = event.filterIsInstance<OwnershipDeleteEventDto>().map {
            it.ownershipId.fullId()
        }

        if (events.isNotEmpty()) {
            logger.debug("Saving ${events.size} OwnershipDto events to ElasticSearch")
            val refreshPolicy =
                if (featureFlagsProperties.enableOwnershipSaveImmediateToElasticSearch) {
                    WriteRequest.RefreshPolicy.IMMEDIATE
                }
                else {
                    WriteRequest.RefreshPolicy.NONE
                }
            repository.saveAll(events, refreshPolicy = refreshPolicy)
            countSaves(events)
        }

        if (deleted.isNotEmpty()) {
            logger.debug("Removing ${deleted.size} OwnershipDto events from ElasticSearch")
            repository.deleteAll(deleted)
        }

        val elapsedTime = nowMillis().minusMillis(startTime.toEpochMilli()).toEpochMilli()
        logger.info("Handling of ${event.size} OwnershipDto events completed in $elapsedTime ms")
    }

    private fun countSaves(ownerships: List<EsOwnership>) {
        val countByBlockchain = ownerships.groupingBy { it.blockchain }.eachCount()
        countByBlockchain.entries.forEach {
            entitySaveCounters[it.key]!!.increment(it.value.toDouble())
        }
    }
}
