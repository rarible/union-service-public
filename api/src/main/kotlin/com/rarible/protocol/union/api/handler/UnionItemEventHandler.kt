package com.rarible.protocol.union.api.handler

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

@Component
class UnionItemEventHandler : IncomingEventHandler<UnionItemEvent> {

    private val sink = Sinks.many().multicast().directBestEffort<UnionItemEvent>()
    val updates: Flux<UnionItemEvent> = sink.asFlux()

    private val logger = LoggerFactory.getLogger(javaClass)


    override suspend fun onEvent(event: UnionItemEvent) {
        logger.debug("Event received: {}", event)
        sink.tryEmitNext(event)
    }
}
