package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.converter.EsItemConverter.toEsItem
import com.rarible.protocol.union.dto.ItemDeleteEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import org.elasticsearch.action.support.WriteRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ItemEventHandler(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val repository: EsItemRepository
) : ConsumerBatchEventHandler<ItemEventDto> {

    private val logger: Logger = LoggerFactory.getLogger(ItemEventHandler::class.java)

    override suspend fun handle(event: List<ItemEventDto>) {
        logger.info("Handling ${event.size} ItemEventDto events")

        val convertedEvents = event
            .filterIsInstance<ItemUpdateEventDto>()
            .map {
                it as ItemUpdateEventDto
                logger.debug("Converting ItemDto id = ${it.itemId.fullId()}")
                it.item.toEsItem()
            }
        logger.debug("Saving ${convertedEvents.size} ItemUpdateEventDto events to ElasticSearch")
        val refreshPolicy =
            if (featureFlagsProperties.enableItemSaveImmediateToElasticSearch) {
                WriteRequest.RefreshPolicy.IMMEDIATE
            }
            else {
                WriteRequest.RefreshPolicy.NONE
            }

        repository.saveAll(convertedEvents, refreshPolicy = refreshPolicy)

        val deletedIds = event
            .filterIsInstance<ItemDeleteEventDto>()
            .map {
                it.itemId.fullId()
            }
        logger.debug("Deleting ${deletedIds.size} ItemDeleteEventDto events to ElasticSearch")
        repository.deleteAll(deletedIds)
        logger.info("Handling completed")
    }
}
