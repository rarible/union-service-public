package com.rarible.protocol.union.listener.handler.internal

import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.core.model.UnionInternalActivityEvent
import com.rarible.protocol.union.core.model.UnionInternalAuctionEvent
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.core.model.UnionInternalCollectionEvent
import com.rarible.protocol.union.core.model.UnionInternalItemEvent
import com.rarible.protocol.union.core.model.UnionInternalOrderEvent
import com.rarible.protocol.union.core.model.UnionInternalOwnershipEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Internal handler for item-related events (item/ownership/order/auction/activity)
 */
@Component
class UnionWrappedEventHandler(
    private val internalItemEventHandler: UnionInternalItemEventHandler,
    private val internalOwnershipEventHandler: UnionInternalOwnershipEventHandler,
    private val internalOrderEventHandler: UnionInternalOrderEventHandler,
    private val internalAuctionEventHandler: UnionInternalAuctionEventHandler,
    private val internalActivityEventHandler: UnionInternalActivityEventHandler,
    private val internalCollectionEventHandler: UnionInternalCollectionEventHandler
) : InternalEventHandler<UnionInternalBlockchainEvent> {

    override suspend fun handle(event: UnionInternalBlockchainEvent) {
        when (event) {
            is UnionInternalItemEvent -> internalItemEventHandler.onEvent(event.event)
            is UnionInternalOwnershipEvent -> internalOwnershipEventHandler.onEvent(event.event)
            is UnionInternalOrderEvent -> internalOrderEventHandler.onEvent(event.event)
            is UnionInternalAuctionEvent -> internalAuctionEventHandler.onEvent(event.event)
            is UnionInternalActivityEvent -> internalActivityEventHandler.onEvent(event.event)
            is UnionInternalCollectionEvent -> internalCollectionEventHandler.onEvent(event.event)
        }
    }

    private val logger = LoggerFactory.getLogger(UnionWrappedEventHandler::class.java)
}