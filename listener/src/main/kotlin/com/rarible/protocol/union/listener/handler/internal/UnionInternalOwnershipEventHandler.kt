package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipEventService
import org.springframework.stereotype.Component

@Component
class UnionInternalOwnershipEventHandler(
    private val ownershipEventService: EnrichmentOwnershipEventService,
    private val reconciliationEventService: ReconciliationEventService
) {

    @CaptureTransaction("UnionOwnershipEvent")
    suspend fun onEvent(event: UnionOwnershipEvent) {
        try {
            when (event) {
                is UnionOwnershipUpdateEvent -> {
                    ownershipEventService.onOwnershipUpdated(event.ownership)
                }
                is UnionOwnershipDeleteEvent -> {
                    ownershipEventService.onOwnershipDeleted(event.ownershipId)
                }
            }
        } catch (e: Throwable) {
            reconciliationEventService.onCorruptedOwnership(event.ownershipId)
            throw e
        }
    }
}