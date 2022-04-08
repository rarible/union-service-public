package com.rarible.protocol.union.api.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import com.rarible.protocol.union.dto.websocket.ChangeEvent
import com.rarible.protocol.union.dto.websocket.AbstractSubscribeRequest
import com.rarible.protocol.union.dto.websocket.ChangeEventType
import com.rarible.protocol.union.dto.websocket.SubscribeRequestType
import com.rarible.protocol.union.dto.websocket.SubscribeRequest
import com.rarible.protocol.union.dto.websocket.SubscriptionAction

@Component
class ChangesHandler(
    private val objectMapper: ObjectMapper,
    private val itemUpdateListener: UnionItemEventHandler,
    private val ownershipUpdateListener: UnionOwnershipEventHandler
) : WebSocketHandler {

   private val fake = objectMapper.writeValueAsString(ChangeEvent(ChangeEventType.FAKE, null))

    override fun handle(session: WebSocketSession): Mono<Void> {
        val subscribedItems = ConcurrentHashMap.newKeySet<String>()
        val subscribedOwnerships = ConcurrentHashMap.newKeySet<String>()

        return Mono.`when`(
            session.receive().doOnNext { message ->
                try {
                    for (request in objectMapper.readValue<List<AbstractSubscribeRequest>>(message.payloadAsText)) {
                        when (request.type) {
                            SubscribeRequestType.ITEM -> handleSubscriptionRequest(request, subscribedItems)
                            SubscribeRequestType.OWNERSHIP -> handleSubscriptionRequest(request, subscribedOwnerships)
                        }
                    }
                } catch (ex: Throwable) {
                    logger.error("Unable to read message", ex)
                }
            },

            session.send(
                Flux.merge(
                    Flux.just(0L)
                        .mergeWith(Flux.interval(Duration.ofSeconds(50)))
                        .map { session.textMessage(fake) },

                    itemUpdateListener.updates
                        .filter { it.itemId.value in subscribedItems}
                        .map { toMessage(session, ChangeEventType.ITEM, it) },

                   ownershipUpdateListener.updates
                        .filter { it.ownershipId.value in subscribedOwnerships }
                        .map { toMessage(session, ChangeEventType.OWNERSHIP, it) }
                )
            )
        )
    }

    private fun handleSubscriptionRequest(
        abstractRequest: AbstractSubscribeRequest,
        subscribedIds: ConcurrentHashMap.KeySetView<String, Boolean>
    ) {
        val request = abstractRequest as SubscribeRequest
        when (request.action) {
            SubscriptionAction.SUBSCRIBE -> subscribedIds.add(request.id)
            SubscriptionAction.UNSUBSCRIBE -> subscribedIds.remove(request.id)
        }
    }

    private fun toMessage(session: WebSocketSession, type: ChangeEventType, value: Any): WebSocketMessage =
        session.textMessage(objectMapper.writeValueAsString(ChangeEvent(type, value)))

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ChangesHandler::class.java)
    }
}

