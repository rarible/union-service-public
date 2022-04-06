package com.rarible.protocol.union.api.handler

import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.dto.ItemEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

@Component
class UnionItemEventHandler: ConsumerEventHandler<ItemEventDto> {

    private val sink = Sinks.many().multicast().directBestEffort<ItemEventDto>()
    val updates: Flux<ItemEventDto> = sink.asFlux()

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: ItemEventDto) {
        logger.debug("Event received: {}", event)
        sink.tryEmitNext(event)
    }
}
