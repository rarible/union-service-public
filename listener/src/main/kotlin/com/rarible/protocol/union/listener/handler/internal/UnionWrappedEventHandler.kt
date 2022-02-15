package com.rarible.protocol.union.listener.handler.internal

import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.core.model.UnionWrappedEvent
import com.rarible.protocol.union.core.model.UnionWrappedItemEvent
import com.rarible.protocol.union.core.model.UnionWrappedOrderEvent
import com.rarible.protocol.union.core.model.UnionWrappedOwnershipEvent
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.stereotype.Component

/**
 * Internal wrapped handler for item-related events (item/ownership/order)
 */
@Component
class UnionWrappedEventHandler(
    private val wrappedItemEventHandler: UnionWrappedItemEventHandler,
    private val wrappedOwnershipEventHandler: UnionWrappedOwnershipEventHandler,
    private val wrappedOrderEventHandler: UnionWrappedOrderEventHandler
) : InternalEventHandler<UnionWrappedEvent> {

    override suspend fun handle(event: UnionWrappedEvent) {
        when (event) {
            is UnionWrappedItemEvent -> if (event.event.itemId.blockchain != BlockchainDto.FLOW) {
                wrappedItemEventHandler.onEvent(event.event)
            }
            is UnionWrappedOwnershipEvent -> if (event.event.ownershipId.blockchain != BlockchainDto.FLOW) {
                wrappedOwnershipEventHandler.onEvent(event.event)
            }
            is UnionWrappedOrderEvent -> if (event.event.orderId.blockchain != BlockchainDto.FLOW) {
                wrappedOrderEventHandler.onEvent(event.event)
            }
        }
    }
}