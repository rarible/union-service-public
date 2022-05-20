package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.protocol.union.core.converter.EsCollectionConverter
import com.rarible.protocol.union.core.converter.EsItemConverter
import com.rarible.protocol.union.core.converter.EsItemConverter.toEsItem
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ItemEventHandler(
    private val repository: EsItemRepository
): ConsumerBatchEventHandler<ItemEventDto> {

    private val logger: Logger = LoggerFactory.getLogger(ItemEventHandler::class.java)

    override suspend fun handle(event: List<ItemEventDto>) {
        logger.info("Handling ${event.size} ItemEventDto events")

        val convertedEvents = event.map {
            it as ItemUpdateEventDto
            logger.debug("Converting ItemDto id = ${it.itemId.fullId()}")
            it.item.toEsItem()
        }
        logger.debug("Saving ${convertedEvents.size} ItemEventDto events to ElasticSearch")
        repository.saveAll(convertedEvents)
        logger.info("Handling completed")
    }
}
