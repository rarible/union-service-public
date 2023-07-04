package com.rarible.protocol.union.core.handler

import com.rarible.core.apm.withTransaction
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.util.capitalise
import com.rarible.protocol.union.dto.BlockchainDto
import org.slf4j.LoggerFactory

abstract class AbstractBlockchainEventHandler<B, U>(
    final override val blockchain: BlockchainDto,
    final override val eventType: EventType
) : BlockchainEventHandler<B, U> {

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractBlockchainEventHandler::class.java)
    }

    // Like ItemEvent#POLYGON
    private val singleEventTransactionName = "${capitalise(eventType.value)}Event#$blockchain"

    // Like ActivityEvents#TEZOS
    private val multipleEventTransactionName = "${capitalise(eventType.value)}Events#$blockchain"

    abstract suspend fun convert(event: B): U?

    override suspend fun handle(event: B) = withTransaction<Unit>(singleEventTransactionName) {
        val unionEvent = convertWithExceptionHandler(event)
        unionEvent?.let { handler.onEvent(it) }
    }

    override suspend fun handle(events: List<B>) = withTransaction(singleEventTransactionName) {
        val start = System.currentTimeMillis()
        val unionEvents = events.mapNotNull { convertWithExceptionHandler(it) }
        if (unionEvents.isNotEmpty()) {
            handler.onEvents(unionEvents)
        }
        logger.info("Handled {} incoming {} events ({}ms)", events.size, eventType, System.currentTimeMillis() - start)
    }

    private suspend fun convertWithExceptionHandler(blockchainEvent: B): U? {
        try {
            return convert(blockchainEvent)
        } catch (e: Exception) {
            throw EventConversionException(blockchainEvent, e)
        }
    }
}
