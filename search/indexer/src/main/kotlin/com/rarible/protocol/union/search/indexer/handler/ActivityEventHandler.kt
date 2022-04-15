package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import org.springframework.stereotype.Service

@Service
class ActivityEventHandler(
    private val repository: EsActivityRepository
): ConsumerBatchEventHandler<ActivityDto> {

    private val logger by Logger()

    override suspend fun handle(events: List<ActivityDto>) {
        logger.info("Handling ${events.size} ActivityDto events")

        val convertedEvents = events.map { event ->
            logger.debug("Converting ActivityDto id = ${event.id}")
            EsActivityConverter.convert(event)
        }
        logger.debug("Saving ${convertedEvents.size} ActivityDto events to ElasticSearch")
        repository.saveAll(convertedEvents)
        logger.info("Handling completed")
    }
}