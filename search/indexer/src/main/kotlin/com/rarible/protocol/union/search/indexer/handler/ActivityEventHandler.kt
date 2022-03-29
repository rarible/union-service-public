package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.search.core.converter.ElasticActivityConverter
import com.rarible.protocol.union.search.core.repository.ActivityEsRepository
import kotlinx.coroutines.reactive.awaitLast
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ActivityEventHandler(
    private val converter: ElasticActivityConverter,
    private val repository: ActivityEsRepository,
): ConsumerBatchEventHandler<ActivityDto> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(events: List<ActivityDto>) {
        logger.info("Handling ${events.size} ActivityDto events")

        val convertedEvents = events.map { event ->
            logger.debug("Converting ActivityDto id = ${event.id}")
            converter.convert(event)
        }
        logger.debug("Saving ${convertedEvents.size} ActivityDto events to ElasticSearch")
        repository.saveAll(convertedEvents).awaitLast()
        logger.info("Handling completed")
    }
}