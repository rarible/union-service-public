package com.rarible.protocol.union.api.handler

import com.rarible.core.kafka.RaribleKafkaEventHandler
import com.rarible.protocol.union.dto.OrderEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

@Component
class UnionSubscribeOrderEventHandler : RaribleKafkaEventHandler<OrderEventDto> {

    private val sink = Sinks.many().multicast().directBestEffort<OrderEventDto>()
    val updates: Flux<OrderEventDto> = sink.asFlux()

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: OrderEventDto) {
        logger.debug("Event received: {}", event)
        sink.tryEmitNext(event)
    }
}
