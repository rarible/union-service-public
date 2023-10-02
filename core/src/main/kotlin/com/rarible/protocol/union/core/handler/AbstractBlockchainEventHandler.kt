package com.rarible.protocol.union.core.handler

import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.dto.BlockchainDto

abstract class AbstractBlockchainEventHandler<B, U>(
    final override val blockchain: BlockchainDto,
    final override val eventType: EventType
) : BlockchainEventHandler<B, U> {

    abstract suspend fun convert(event: B): U?

    override suspend fun handle(event: B) {
        val unionEvent = convertWithExceptionHandler(event)
        unionEvent?.let { handler.onEvent(it) }
    }

    override suspend fun handle(events: List<B>) {
        val unionEvents = events.mapNotNull { convertWithExceptionHandler(it) }
        if (unionEvents.isNotEmpty()) {
            handler.onEvents(unionEvents)
        }
    }

    private suspend fun convertWithExceptionHandler(blockchainEvent: B): U? {
        try {
            return convert(blockchainEvent)
        } catch (e: Exception) {
            throw EventConversionException(blockchainEvent, e)
        }
    }
}
