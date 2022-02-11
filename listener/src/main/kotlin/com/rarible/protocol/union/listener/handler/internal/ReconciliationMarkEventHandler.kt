package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.enrichment.model.ReconciliationItemMarkEvent
import com.rarible.protocol.union.enrichment.model.ReconciliationMark
import com.rarible.protocol.union.enrichment.model.ReconciliationMarkAbstractEvent
import com.rarible.protocol.union.enrichment.model.ReconciliationMarkEvent
import com.rarible.protocol.union.enrichment.model.ReconciliationMarkType
import com.rarible.protocol.union.enrichment.model.ReconciliationOwnershipMarkEvent
import com.rarible.protocol.union.enrichment.repository.ReconciliationMarkRepository
import org.springframework.stereotype.Component

@Component
class ReconciliationMarkEventHandler(
    private val itemReconciliationMarkRepository: ReconciliationMarkRepository
) : InternalEventHandler<ReconciliationMarkAbstractEvent> {

    override suspend fun handle(event: ReconciliationMarkAbstractEvent) {
        val mark = when (event) {
            // TODO remove later
            is ReconciliationItemMarkEvent -> {
                ReconciliationMark(
                    id = event.itemId.fullId(),
                    type = ReconciliationMarkType.ITEM,
                    lastUpdatedAt = nowMillis()
                )
            }
            // TODO remove later
            is ReconciliationOwnershipMarkEvent -> {
                ReconciliationMark(
                    id = event.ownershipId.fullId(),
                    type = ReconciliationMarkType.OWNERSHIP,
                    lastUpdatedAt = nowMillis()
                )
            }
            is ReconciliationMarkEvent -> {
                ReconciliationMark(
                    id = event.entityId,
                    type = event.type,
                    lastUpdatedAt = nowMillis()
                )
            }
        }
        if (itemReconciliationMarkRepository.get(mark.id) == null) {
            itemReconciliationMarkRepository.save(mark)
        }
    }
}