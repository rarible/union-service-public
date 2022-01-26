package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.enrichment.model.ItemReconciliationMark
import com.rarible.protocol.union.enrichment.model.OwnershipReconciliationMark
import com.rarible.protocol.union.enrichment.model.ReconciliationItemMarkEvent
import com.rarible.protocol.union.enrichment.model.ReconciliationMarkEvent
import com.rarible.protocol.union.enrichment.model.ReconciliationOwnershipMarkEvent
import com.rarible.protocol.union.enrichment.repository.ItemReconciliationMarkRepository
import com.rarible.protocol.union.enrichment.repository.OwnershipReconciliationMarkRepository
import org.springframework.stereotype.Component

@Component
class ReconciliationMarkEventHandler(
    private val itemReconciliationMarkRepository: ItemReconciliationMarkRepository,
    private val ownershipReconciliationMarkRepository: OwnershipReconciliationMarkRepository
) : InternalEventHandler<ReconciliationMarkEvent> {

    override suspend fun handle(event: ReconciliationMarkEvent) {
        when (event) {
            is ReconciliationItemMarkEvent -> {
                itemReconciliationMarkRepository.save(
                    ItemReconciliationMark(
                        blockchain = event.itemId.blockchain,
                        token = event.itemId.contract,
                        tokenId = event.itemId.tokenId,
                        lastUpdatedAt = nowMillis()
                    )
                )
            }
            is ReconciliationOwnershipMarkEvent -> {
                ownershipReconciliationMarkRepository.save(
                    OwnershipReconciliationMark(
                        blockchain = event.ownershipId.blockchain,
                        token = event.ownershipId.contract,
                        tokenId = event.ownershipId.tokenId,
                        owner = event.ownershipId.owner.value,
                        lastUpdatedAt = nowMillis()
                    )
                )
            }
        }
    }
}