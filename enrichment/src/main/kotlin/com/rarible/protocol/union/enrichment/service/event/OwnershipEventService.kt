package com.rarible.protocol.union.enrichment.service.event

import com.rarible.core.common.nowMillis
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.event.OwnershipEventDelete
import com.rarible.protocol.union.enrichment.event.OwnershipEventListener
import com.rarible.protocol.union.enrichment.event.OwnershipEventUpdate
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.BestOrderService
import com.rarible.protocol.union.enrichment.service.OwnershipService
import com.rarible.protocol.union.enrichment.util.spent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OwnershipEventService(
    private val ownershipService: OwnershipService,
    private val itemEventService: ItemEventService,
    private val ownershipEventListeners: List<OwnershipEventListener>,
    private val bestOrderService: BestOrderService
) {
    private val logger = LoggerFactory.getLogger(OwnershipEventService::class.java)

    suspend fun onOwnershipUpdated(ownership: OwnershipDto) {
        val received = ShortOwnershipConverter.convert(ownership)
        val existing = ownershipService.getOrEmpty(received.id)
        notifyUpdate(existing, ownership)
    }

    suspend fun onOwnershipBestSellOrderUpdated(ownershipId: ShortOwnershipId, order: OrderDto) = optimisticLock {
        val current = ownershipService.get(ownershipId)
        val exist = current != null
        val short = current ?: ShortOwnership.empty(ownershipId)

        val updated = short.copy(bestSellOrder = bestOrderService.getBestSellOrder(short, order))

        if (short != updated) {
            if (updated.isNotEmpty()) {
                val saved = ownershipService.save(updated)
                notifyUpdate(saved, null, order)
                itemEventService.onOwnershipUpdated(ownershipId, order)
            } else if (exist) {
                logger.info("Deleting Ownership [{}] without related bestSellOrder", ownershipId)
                ownershipService.delete(ownershipId)
                notifyUpdate(updated, null, order)
                itemEventService.onOwnershipUpdated(ownershipId, order)
            }
        } else {
            logger.info("Ownership [{}] not changed after order updated, event won't be published", ownershipId)
        }
    }

    suspend fun onOwnershipDeleted(ownershipId: ShortOwnershipId) {
        logger.debug("Deleting Ownership [{}] since it was removed from NFT-Indexer", ownershipId)
        val deleted = deleteOwnership(ownershipId)
        notifyDelete(ownershipId)
        if (deleted) {
            logger.info("Ownership [{}] deleted (removed from NFT-Indexer), refreshing sell stats", ownershipId)
            itemEventService.onOwnershipUpdated(ownershipId, null)
        }
    }

    private suspend fun updateOwnership(updated: ShortOwnership): ShortOwnership {
        val now = nowMillis()
        val result = ownershipService.save(updated)
        logger.info(
            "Updating Ownership [{}] with enrichment data: bestSellOrder = [{}] ({}ms)",
            updated.id, updated.bestSellOrder?.id, spent(now)
        )
        return result
    }

    private suspend fun deleteOwnership(ownershipId: ShortOwnershipId): Boolean {
        val result = ownershipService.delete(ownershipId)
        return result != null && result.deletedCount > 0
    }

    private suspend fun notifyDelete(ownershipId: ShortOwnershipId) {
        val event = OwnershipEventDelete(ownershipId.toDto())
        ownershipEventListeners.forEach { it.onEvent(event) }
    }

    private suspend fun notifyUpdate(
        short: ShortOwnership,
        ownership: OwnershipDto? = null,
        order: OrderDto? = null
    ) {
        val dto = ownershipService.enrichOwnership(short, ownership, order)
        val event = OwnershipEventUpdate(dto)
        ownershipEventListeners.forEach { it.onEvent(event) }
    }
}