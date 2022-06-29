package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.core.model.ReconciliationMarkEvent
import com.rarible.protocol.union.enrichment.model.ReconciliationMark
import com.rarible.protocol.union.enrichment.repository.ReconciliationMarkRepository
import org.springframework.stereotype.Component

@Component
class ReconciliationMarkEventHandler(
    private val itemReconciliationMarkRepository: ReconciliationMarkRepository
) : InternalEventHandler<ReconciliationMarkEvent> {

    override suspend fun handle(event: ReconciliationMarkEvent) {
        val mark = ReconciliationMark(
            id = event.entityId,
            type = event.type,
            lastUpdatedAt = nowMillis()
        )

        if (itemReconciliationMarkRepository.get(mark.id) == null) {
            itemReconciliationMarkRepository.save(mark)
        }
    }
}
