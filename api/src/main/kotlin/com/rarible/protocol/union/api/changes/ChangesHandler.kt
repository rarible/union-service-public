package com.rarible.api.changes

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.apiservice.updatelistener.ItemUpdateListener
import com.rarible.protocol.union.api.dto.*
import com.rarible.protocol.union.api.updatelistener.OwnershipUpdateListener
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
import javax.swing.event.ChangeEvent

@Component
class ChangesHandler(
    private val objectMapper: ObjectMapper,
    private val itemUpdateListener: ItemUpdateListener,
    private val ownershipUpdateListener: OwnershipUpdateListener
) : WebSocketHandler {

   private val fake = objectMapper.writeValueAsString(ChangeEvent(ChangeEventType.FAKE))

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

