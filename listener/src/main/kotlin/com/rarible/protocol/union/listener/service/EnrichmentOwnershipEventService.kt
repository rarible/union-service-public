package com.rarible.protocol.union.listener.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.event.OutgoingOwnershipEventListener
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.AuctionContractService
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.BestOrderService
import com.rarible.protocol.union.enrichment.service.EnrichmentAuctionService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.validator.OwnershipValidator
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class EnrichmentOwnershipEventService(
    private val enrichmentOwnershipService: EnrichmentOwnershipService,
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentAuctionService: EnrichmentAuctionService,
    private val ownershipEventListeners: List<OutgoingOwnershipEventListener>,
    private val bestOrderService: BestOrderService,
    private val auctionContractService: AuctionContractService
    private val reconciliationEventService: ReconciliationEventService
) {
    private val logger = LoggerFactory.getLogger(EnrichmentOwnershipEventService::class.java)

    suspend fun onOwnershipUpdated(ownership: UnionOwnership) {
        val received = ShortOwnershipConverter.convert(ownership)
        val existing = enrichmentOwnershipService.getOrEmpty(received.id)
        notifyUpdate(existing, ownership)
    }

    suspend fun recalculateBestOrder(ownership: ShortOwnership): Boolean {
        val updated = bestOrderService.updateBestSellOrder(ownership)
        if (ownership.bestSellOrder != updated.bestSellOrder) {
            logger.info(
                "Ownership BestSellOrder updated ([{}] -> [{}]) due to currency rate changed",
                ownership.bestSellOrder?.dtoId, updated.bestSellOrder?.dtoId
            )

            val saved = enrichmentOwnershipService.save(updated)
            notifyUpdate(saved, null, null)
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
        val current = enrichmentOwnershipService.get(ownershipId)
        val exist = current != null
        val short = current ?: ShortOwnership.empty(ownershipId)

        val updated = bestOrderService.updateBestSellOrder(short, order)

        if (short != updated) {
            if (updated.isNotEmpty()) {
                val saved = enrichmentOwnershipService.save(updated)
                if (notificationEnabled) {
                    notifyUpdate(saved, null, order)
                }
                enrichmentItemEventService.onOwnershipUpdated(ownershipId, order, notificationEnabled)
            } else if (exist) {
                logger.info("Deleting Ownership [{}] without related bestSellOrder", ownershipId)
                enrichmentOwnershipService.delete(ownershipId)
                if (notificationEnabled) {
                    notifyUpdate(updated, null, order)
                }
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
        notifyDelete(shortOwnershipId)
        if (deleted) {
            logger.info("Ownership [{}] deleted (removed from NFT-Indexer), refreshing sell stats", shortOwnershipId)
            enrichmentItemEventService.onOwnershipUpdated(shortOwnershipId, null)
        }
    }

    private suspend fun deleteOwnership(ownershipId: ShortOwnershipId): Boolean {
        val result = enrichmentOwnershipService.delete(ownershipId)
        return result != null && result.deletedCount > 0
    }

    private suspend fun notifyDelete(ownershipId: ShortOwnershipId) {
        val event = OwnershipDeleteEventDto(
            eventId = UUID.randomUUID().toString(),
            ownershipId = ownershipId.toDto()
        )
        ownershipEventListeners.forEach { it.onEvent(event) }
    }

    private suspend fun notifyUpdate(
        short: ShortOwnership,
        ownership: UnionOwnership? = null,
        order: OrderDto? = null
    ) {
        val isAuctionOwnership = (auctionContractService.isAuctionContract(short.blockchain, short.owner))
        if (isAuctionOwnership) {
            // We should skip auction ownerships
            return
        }

        val dto = coroutineScope {
            val auctionDeferred = async { enrichmentAuctionService.fetchOwnershipAuction(short.id) }
            val orders = listOfNotNull(order).associateBy { it.id }
            val enriched = enrichmentOwnershipService.enrichOwnership(short, ownership, orders)
            enrichmentOwnershipService.mergeWithAuction(enriched, auctionDeferred.await())
        }

        val event = OwnershipUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            ownershipId = short.id.toDto(),
            ownership = dto
        )

        if (!OwnershipValidator.isValid(dto)) {
            reconciliationEventService.onCorruptedOwnership(dto.id)
        }
        ownershipEventListeners.forEach { it.onEvent(event) }
    }
}
