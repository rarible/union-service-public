package com.rarible.protocol.union.api.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.protocol.union.dto.FakeSubscriptionEventDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemSubscriptionEventDto
import com.rarible.protocol.union.dto.ItemSubscriptionRequestDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipSubscriptionEventDto
import com.rarible.protocol.union.dto.OwnershipSubscriptionRequestDto
import com.rarible.protocol.union.dto.SubscriptionActionDto
import com.rarible.protocol.union.dto.SubscriptionRequestDto
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

@Component
class ChangesHandler(
    private val objectMapper: ObjectMapper,
    private val itemUpdateListener: UnionItemEventHandler,
    private val ownershipUpdateListener: UnionOwnershipEventHandler
) : WebSocketHandler {

   private val fake = objectMapper.writeValueAsString(FakeSubscriptionEventDto())

    override fun handle(session: WebSocketSession): Mono<Void> {
        val subscribedItems = ConcurrentHashMap.newKeySet<ItemIdDto>()
        val subscribedOwnerships = ConcurrentHashMap.newKeySet<OwnershipIdDto>()

        return Mono.`when`(
            session.receive().doOnNext { message ->
                try {
                    for (request in objectMapper.readValue<List<SubscriptionRequestDto>>(message.payloadAsText)) {
                        when (request) {
                            is ItemSubscriptionRequestDto -> handleSubscriptionRequest(request.action, request.id, subscribedItems)
                            is OwnershipSubscriptionRequestDto -> handleSubscriptionRequest(request.action, request.id, subscribedOwnerships)
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
                        .filter { it.itemId in subscribedItems }
                        .map { toMessage(session, ItemSubscriptionEventDto(it)) },

                    ownershipUpdateListener.updates
                        .filter { it.ownershipId in subscribedOwnerships }
                        .map { toMessage(session, OwnershipSubscriptionEventDto(it)) }
                )
            )
        )
    }

    private fun <T> handleSubscriptionRequest(
        action: SubscriptionActionDto,
        id: T,
        subscribedIds: ConcurrentHashMap.KeySetView<T, Boolean>
    ) {
        when (action) {
            SubscriptionActionDto.SUBSCRIBE -> subscribedIds.add(id)
            SubscriptionActionDto.UNSUBSCRIBE -> subscribedIds.remove(id)
        }
    }

    private fun toMessage(session: WebSocketSession, event: Any): WebSocketMessage =
        session.textMessage(objectMapper.writeValueAsString(event))

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ChangesHandler::class.java)
    }
}

