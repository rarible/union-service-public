package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.model.UnionOwnershipChangeEvent
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipEventService
import com.rarible.protocol.union.enrichment.service.ReconciliationEventService
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
                is UnionOwnershipChangeEvent -> {
                    ownershipEventService.onOwnershipChanged(event)
                }

                is UnionOwnershipUpdateEvent -> {
                    ownershipEventService.onOwnershipUpdated(event)
                }

                is UnionOwnershipDeleteEvent -> {
                    ownershipEventService.onOwnershipDeleted(event)
                }
            }
        } catch (e: Throwable) {
            reconciliationEventService.onCorruptedOwnership(event.ownershipId)
            throw e
        }
    }
}
