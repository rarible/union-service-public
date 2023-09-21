package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.common.asyncWithTraceId
import com.rarible.protocol.union.core.handler.InternalBatchEventHandler
import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.core.model.UnionInternalActivityEvent
import com.rarible.protocol.union.core.model.UnionInternalAuctionEvent
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.core.model.UnionInternalCollectionEvent
import com.rarible.protocol.union.core.model.UnionInternalItemEvent
import com.rarible.protocol.union.core.model.UnionInternalOrderEvent
import com.rarible.protocol.union.core.model.UnionInternalOwnershipEvent
import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Internal handler for item-related events (item/ownership/order/auction/activity)
 */
@Component
class UnionInternalEventHandler(
    private val internalItemEventHandler: UnionInternalItemEventHandler,
    private val internalOwnershipEventHandler: UnionInternalOwnershipEventHandler,
    private val internalOrderEventHandler: UnionInternalOrderEventHandler,
    private val internalAuctionEventHandler: UnionInternalAuctionEventHandler,
    private val internalActivityEventHandler: UnionInternalActivityEventHandler,
    private val internalCollectionEventHandler: UnionInternalCollectionEventHandler
) : InternalEventHandler<UnionInternalBlockchainEvent> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: UnionInternalBlockchainEvent) {
        try {
            when (event) {
                is UnionInternalItemEvent -> internalItemEventHandler.onEvent(event.event)
                is UnionInternalOwnershipEvent -> internalOwnershipEventHandler.onEvent(event.event)
                is UnionInternalOrderEvent -> internalOrderEventHandler.onEvent(event.event)
                is UnionInternalAuctionEvent -> internalAuctionEventHandler.onEvent(event.event)
                is UnionInternalActivityEvent -> internalActivityEventHandler.onEvent(event.event)
                is UnionInternalCollectionEvent -> internalCollectionEventHandler.onEvent(event.event)
            }
        } catch (ex: Exception) {
            logger.error("Unexpected exception during handling internal event $event", ex)
        }
    }
}

class UnionInternalChunkedEventHandler(
    private val handler: UnionInternalEventHandler,
    private val unionInternalEventChunker: UnionInternalEventChunker,
    private val blockchain: BlockchainDto
) : InternalBatchEventHandler<UnionInternalBlockchainEvent>, InternalEventHandler<UnionInternalBlockchainEvent> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: List<UnionInternalBlockchainEvent>) {
        val start = System.currentTimeMillis()

        // Here events divided in chunks that should be handled consequently,
        // but events inside each chunk can be handled in parallel
        // (initially done for case when we got tons of ownerships for same item - they can be handled in parallel)
        val sequentialChunks = unionInternalEventChunker.toChunks(event)
        coroutineScope {
            sequentialChunks.forEach { chunk ->
                if (chunk.size == 1) {
                    handle(chunk.first())
                } else {
                    chunk.map { event ->
                        asyncWithTraceId(context = NonCancellable) {
                            handle(event)
                        }
                    }.awaitAll()
                }
            }
        }

        val spent = System.currentTimeMillis() - start

        // Just to avoid spam
        if (spent > 200) {
            logger.info(
                "Handled {} internal events in {} chunks for {} ({}ms)",
                event.size,
                sequentialChunks.size,
                blockchain,
                spent
            )
        }
    }

    override suspend fun handle(event: UnionInternalBlockchainEvent) {
        handler.handle(event)
    }
}
