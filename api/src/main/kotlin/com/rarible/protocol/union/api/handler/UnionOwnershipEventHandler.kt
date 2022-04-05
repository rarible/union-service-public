package com.rarible.protocol.union.api.handler

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

@Component
class UnionOwnershipEventHandler : IncomingEventHandler<UnionOwnershipEvent> {

    private val sink = Sinks.many().multicast().directBestEffort<UnionOwnershipEvent>()
    val updates: Flux<UnionOwnershipEvent> = sink.asFlux()

    private val logger = LoggerFactory.getLogger(javaClass)


    override suspend fun onEvent(event: UnionOwnershipEvent) {
        logger.debug("Event received: {}", event)
        sink.tryEmitNext(event)
    }
}
