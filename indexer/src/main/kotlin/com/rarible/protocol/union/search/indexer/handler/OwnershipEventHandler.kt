package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.converter.EsOwnershipConverter
import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

        val events = event.filterIsInstance<OwnershipUpdateEventDto>().map {
            logger.debug("Converting OwnershipDto id = ${it.ownershipId}")
            EsOwnershipConverter.convert(it.ownership)
        }

        val deleted = event.filterIsInstance<OwnershipDeleteEventDto>().map {
            it.ownershipId.fullId()
        }

        coroutineScope {
            listOf(
                async {
                    logger.debug("Saving ${events.size} OwnershipDto events to ElasticSearch")
                    val refreshPolicy =
                        if (featureFlagsProperties.enableOwnershipSaveImmediateToElasticSearch) {
                            WriteRequest.RefreshPolicy.IMMEDIATE
                        }
                        else {
                            WriteRequest.RefreshPolicy.NONE
                        }
                    if (events.isNotEmpty()) {
                        repository.saveAll(events, refreshPolicy = refreshPolicy)
                    }
                },
                async {
                    logger.debug("Removing ${deleted.size} OwnershipDto events from ElasticSearch")
                    if (deleted.isNotEmpty()) {
                        repository.deleteAll(deleted)
                    }
                },
            ).awaitAll()
        }

        logger.info("Handling completed")
    }
}
