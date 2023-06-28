package com.rarible.protocol.union.api.handler

import com.rarible.core.kafka.RaribleKafkaEventHandler
import com.rarible.protocol.union.dto.OwnershipEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

@Component
class UnionSubscribeOwnershipEventHandler : RaribleKafkaEventHandler<OwnershipEventDto> {

    private val sink = Sinks.many().multicast().directBestEffort<OwnershipEventDto>()
    val updates: Flux<OwnershipEventDto> = sink.asFlux()

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: OwnershipEventDto) {
        logger.debug("Event received: {}", event)
        sink.tryEmitNext(event)
    }
}
