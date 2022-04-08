package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.search.core.converter.ElasticOrderConverter
import com.rarible.protocol.union.search.core.repository.OrderEsRepository
import kotlinx.coroutines.reactive.awaitLast
import org.springframework.stereotype.Service

@Service
class OrderEventHandler(
    private val converter: ElasticOrderConverter,
    private val repository: OrderEsRepository,
): ConsumerBatchEventHandler<OrderEventDto> {

    private val logger by Logger()

    override suspend fun handle(events: List<OrderEventDto>) {
        logger.debug("Handling ${events.size} ActivityDto events")

        val convertedEvents = events.map { event ->
            logger.debug("Converting OrderDto id = ${event.orderId}")
            converter.convert(event)
        }
        logger.debug("Saving ${convertedEvents.size} OrderDto events to ElasticSearch")
        repository.saveAll(convertedEvents).awaitLast()
        logger.debug("Handling completed")
    }
}
