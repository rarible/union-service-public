package com.rarible.protocol.union.listener.handler.wrapped

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.listener.service.EnrichmentOwnershipEventService
import org.springframework.stereotype.Component

@Component
class UnionWrappedOwnershipEventHandler(
    private val ownershipEventService: EnrichmentOwnershipEventService,
) {

    @CaptureTransaction("UnionOwnershipEvent")
    suspend fun onEvent(event: UnionOwnershipEvent) {
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