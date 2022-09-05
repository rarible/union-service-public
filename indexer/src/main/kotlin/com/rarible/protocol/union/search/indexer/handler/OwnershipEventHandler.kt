package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.converter.EsOwnershipConverter
import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import org.elasticsearch.action.support.WriteRequest
import org.springframework.stereotype.Service

@Service
class OwnershipEventHandler(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val repository: EsOwnershipRepository,
) : ConsumerBatchEventHandler<OwnershipEventDto> {

    private val logger by Logger()

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

        logger.debug("Saving ${events.size} OwnershipDto events to ElasticSearch")
        val refreshPolicy =
            if (featureFlagsProperties.enableOwnershipSaveImmediateToElasticSearch) {
                WriteRequest.RefreshPolicy.IMMEDIATE
            } else {
                WriteRequest.RefreshPolicy.NONE
            }

        repository.bulk(events, deleted, refreshPolicy = refreshPolicy)

        val elapsedTime = nowMillis().minusMillis(startTime.toEpochMilli()).toEpochMilli()
        logger.info(
            "Handling of ${event.size} OwnershipDto events completed in $elapsedTime ms" +
                    " (saved: ${events.size}, deleted: ${deleted.size})"
        )
    }
}
