package com.rarible.protocol.union.listener.handler

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.listener.service.EnrichmentOwnershipEventService
import org.springframework.stereotype.Component

@Component
class UnionOwnershipEventHandler(
    private val ownershipEventService: EnrichmentOwnershipEventService
) : IncomingEventHandler<UnionOwnershipEvent> {

    override suspend fun onEvent(event: UnionOwnershipEvent) {
        when (event) {
            is UnionOwnershipUpdateEvent -> {
                ownershipEventService.onOwnershipUpdated(event.ownership)
            }
            is UnionOwnershipDeleteEvent -> {
                ownershipEventService.onOwnershipDeleted(event.ownershipId)
            }
        }
    }
}
