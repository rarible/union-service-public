package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.core.converter.EsOrderConverter
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import org.springframework.stereotype.Service

@Service
class OrderEventHandler(
    private val repository: EsOrderRepository
): ConsumerBatchEventHandler<OrderEventDto> {

    private val logger by Logger()

    override suspend fun handle(events: List<OrderEventDto>) {
        logger.debug("Handling ${events.size} ActivityDto events")

        val convertedEvents = events.map { event ->
            logger.debug("Converting OrderDto id = ${event.orderId}")
            EsOrderConverter.convert(event)
        }
        logger.debug("Saving ${convertedEvents.size} OrderDto events to ElasticSearch")
        repository.saveAll(convertedEvents)
        logger.debug("Handling completed")
    }
}
