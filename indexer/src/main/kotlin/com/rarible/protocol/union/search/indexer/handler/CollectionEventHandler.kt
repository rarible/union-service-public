package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.protocol.union.core.converter.EsCollectionConverter
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CollectionEventHandler(
    private val repository: EsCollectionRepository
): ConsumerBatchEventHandler<CollectionEventDto> {

    private val logger: Logger = LoggerFactory.getLogger(CollectionEventHandler::class.java)

    override suspend fun handle(event: List<CollectionEventDto>) {
        logger.info("Handling ${event.size} CollectionEventDto events")

        val convertedEvents = event.map {
            it as CollectionUpdateEventDto
            logger.debug("Converting CollectionDto id = ${it.collection.id.fullId()}")
            EsCollectionConverter.convert(it.collection)
        }
        logger.debug("Saving ${convertedEvents.size} CollectionEventDto events to ElasticSearch")
        repository.saveAll(convertedEvents)
        logger.info("Handling completed")
    }
}
