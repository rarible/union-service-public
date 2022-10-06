package com.rarible.protocol.union.api.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.protocol.union.dto.FakeSubscriptionEventDto
import com.rarible.protocol.union.dto.SubscriptionRequestDto
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class ChangesHandler(
    private val objectMapper: ObjectMapper,
    private val subscriptionHandler: SubscriptionHandler,
) : WebSocketHandler {

    private val fake = objectMapper.writeValueAsString(FakeSubscriptionEventDto())

    override fun handle(session: WebSocketSession): Mono<Void> {
        return subscriptionHandler.handle(
            { session.receive().map { objectMapper.readValue<List<SubscriptionRequestDto>>(it.payloadAsText) } },
            {
                session.send(
                    Flux.merge(
                        Flux.just(0L)
                            .mergeWith(Flux.interval(Duration.ofSeconds(50)))
                            .map { fake },
                        it
                    ).map { msg -> toMessage(session, msg) }
                )
            }
        )
    }

    private fun toMessage(session: WebSocketSession, event: Any): WebSocketMessage =
        session.textMessage(objectMapper.writeValueAsString(event))
}
