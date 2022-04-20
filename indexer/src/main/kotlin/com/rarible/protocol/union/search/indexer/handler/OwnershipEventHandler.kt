package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.converter.EsOwnershipConverter
import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import org.springframework.stereotype.Service

@Service
class OwnershipEventHandler(
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

        logger.debug("Saving ${events.size} OwnershipDto events to ElasticSearch")
        repository.saveAll(events)

        logger.debug("Removing ${deleted.size} OwnershipDto events from ElasticSearch")
        repository.deleteAll(deleted)


        logger.info("Handling completed")
    }
}
