package com.rarible.protocol.union.listener.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.event.OutgoingOwnershipEventListener
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.BestOrderService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.validator.OwnershipValidator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class EnrichmentOwnershipEventService(
    private val ownershipService: EnrichmentOwnershipService,
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val ownershipEventListeners: List<OutgoingOwnershipEventListener>,
    private val bestOrderService: BestOrderService,
    private val reconciliationEventService: ReconciliationEventService
) {

    private val logger = LoggerFactory.getLogger(EnrichmentOwnershipEventService::class.java)

    suspend fun onOwnershipUpdated(ownership: UnionOwnership) {
        val existing = ownershipService.getOrEmpty(ShortOwnershipId(ownership.id))
        val event = buildUpdateEvent(existing, ownership)
        sendUpdate(event)
    }

    suspend fun recalculateBestOrder(ownership: ShortOwnership): Boolean {
        val updated = bestOrderService.updateBestSellOrder(ownership)
        if (ownership.bestSellOrder != updated.bestSellOrder) {
            logger.info(
                "Ownership BestSellOrder updated ([{}] -> [{}]) due to currency rate changed",
                ownership.bestSellOrder?.dtoId, updated.bestSellOrder?.dtoId
            )

            saveAndNotify(updated, false)
            enrichmentItemEventService.onOwnershipUpdated(ownership.id, null)
            return true
        }
        return false
    }

    suspend fun onOwnershipBestSellOrderUpdated(
        ownershipId: ShortOwnershipId,
        order: OrderDto,
        notificationEnabled: Boolean = true
    ) = optimisticLock {
        val current = ownershipService.get(ownershipId)
        val exist = current != null
        val short = current ?: ShortOwnership.empty(ownershipId)

        val updated = bestOrderService.updateBestSellOrder(short, order)

        if (short != updated) {
            if (updated.isNotEmpty()) {
                saveAndNotify(updated, notificationEnabled, null, order)
                enrichmentItemEventService.onOwnershipUpdated(ownershipId, order, notificationEnabled)
            } else if (exist) {
                logger.info("Deleting Ownership [{}] without related bestSellOrder", ownershipId)
                cleanupAndNotify(updated, notificationEnabled, null, order)
                enrichmentItemEventService.onOwnershipUpdated(ownershipId, order, notificationEnabled)
            }
        } else {
            logger.info("Ownership [{}] not changed after order updated, event won't be published", ownershipId)
        }
    }

    suspend fun onOwnershipDeleted(ownershipId: OwnershipIdDto) {
        val shortOwnershipId = ShortOwnershipId(ownershipId)
        logger.debug("Deleting Ownership [{}] since it was removed from NFT-Indexer", shortOwnershipId)
        val deleted = deleteOwnership(shortOwnershipId)
        sendDelete(shortOwnershipId)
        if (deleted) {
            logger.info("Ownership [{}] deleted (removed from NFT-Indexer), refreshing sell stats", shortOwnershipId)
            enrichmentItemEventService.onOwnershipUpdated(shortOwnershipId, null)
        }
    }

    private suspend fun deleteOwnership(ownershipId: ShortOwnershipId): Boolean {
        val result = ownershipService.delete(ownershipId)
        return result != null && result.deletedCount > 0
    }

    private suspend fun saveAndNotify(
        updated: ShortOwnership,
        notificationEnabled: Boolean,
        ownership: UnionOwnership? = null,
        order: OrderDto? = null
    ) {
        if (!notificationEnabled) {
            ownershipService.save(updated)
            return
        }

        val event = buildUpdateEvent(updated, ownership, order)
        ownershipService.save(updated)
        sendUpdate(event)
    }

    private suspend fun cleanupAndNotify(
        updated: ShortOwnership,
        notificationEnabled: Boolean,
        ownership: UnionOwnership? = null,
        order: OrderDto? = null
    ) {
        if (!notificationEnabled) {
            ownershipService.delete(updated.id)
            return
        }

        val event = buildUpdateEvent(updated, ownership, order)
        ownershipService.delete(updated.id)
        sendUpdate(event)
    }

    private suspend fun buildUpdateEvent(
        short: ShortOwnership,
        ownership: UnionOwnership? = null,
        order: OrderDto? = null
    ): OwnershipUpdateEventDto {
        val dto = ownershipService.enrichOwnership(short, ownership, listOfNotNull(order).associateBy { it.id })
        return OwnershipUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            ownershipId = short.id.toDto(),
            ownership = dto
        )
    }

    private suspend fun sendUpdate(event: OwnershipUpdateEventDto) {
        // If ownership in corrupted state, we will try to reconcile it instead of sending corrupted
        // data to the customers
        if (!OwnershipValidator.isValid(event.ownership)) {
            reconciliationEventService.onCorruptedOwnership(event.ownership.id)
        } else {
            ownershipEventListeners.forEach { it.onEvent(event) }
        }
    }

    private suspend fun sendDelete(ownershipId: ShortOwnershipId) {
        val event = OwnershipDeleteEventDto(
            eventId = UUID.randomUUID().toString(),
            ownershipId = ownershipId.toDto()
        )
        ownershipEventListeners.forEach { it.onEvent(event) }
    }
}
