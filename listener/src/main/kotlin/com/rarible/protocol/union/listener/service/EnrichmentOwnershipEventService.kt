package com.rarible.protocol.union.listener.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.event.OutgoingOwnershipEventListener
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.BestOrderService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class EnrichmentOwnershipEventService(
    private val ownershipService: EnrichmentOwnershipService,
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val ownershipEventListeners: List<OutgoingOwnershipEventListener>,
    private val bestOrderService: BestOrderService
) {
    private val logger = LoggerFactory.getLogger(EnrichmentOwnershipEventService::class.java)

    suspend fun onOwnershipUpdated(ownership: UnionOwnership) {
        val received = ShortOwnershipConverter.convert(ownership)
        val existing = ownershipService.getOrEmpty(received.id)
        notifyUpdate(existing, ownership)
    }

    suspend fun recalculateBestOrder(ownership: ShortOwnership): Boolean {
        val updated = bestOrderService.updateBestSellOrder(ownership)
        if (ownership.bestSellOrder != updated.bestSellOrder) {
            logger.info(
                "Ownership BestSellOrder updated ([{}] -> [{}]) due to currency rate changed",
                ownership.bestSellOrder?.dtoId, updated.bestSellOrder?.dtoId
            )

            val saved = ownershipService.save(updated)
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
        val current = ownershipService.get(ownershipId)
        val exist = current != null
        val short = current ?: ShortOwnership.empty(ownershipId)

        val updated = bestOrderService.updateBestSellOrder(short, order)

        if (short != updated) {
            if (updated.isNotEmpty()) {
                val saved = ownershipService.save(updated)
                if (notificationEnabled) {
                    notifyUpdate(saved, null, order)
                }
                enrichmentItemEventService.onOwnershipUpdated(ownershipId, order, notificationEnabled)
            } else if (exist) {
                logger.info("Deleting Ownership [{}] without related bestSellOrder", ownershipId)
                ownershipService.delete(ownershipId)
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

    suspend fun onAuctionUpdated(
        ownershipId: ShortOwnershipId,
        auction: AuctionDto,
        notificationEnabled: Boolean = true
    ) = optimisticLock {
        val current = ownershipService.get(ownershipId)
        val exist = current != null
        val short = current ?: ShortOwnership.empty(ownershipId)

        val updated = short.copy(auctions = short.auctions + auction.id)

        if (short != updated) {
            if (updated.isNotEmpty()) {
                val saved = ownershipService.save(updated)
                if (notificationEnabled) {
                    notifyUpdate(saved, null, null, auction)
                }
            } else if (exist) {
                logger.info("Deleting Ownership [{}] without related auction", ownershipId)
                ownershipService.delete(ownershipId)
                if (notificationEnabled) {
                    notifyUpdate(updated, null, null, auction)
                }
            }
        } else {
            logger.info("Ownership [{}] not changed after auction updated, event won't be published", ownershipId)
        }
    }

    suspend fun onAuctionDeleted(
        auctionId: AuctionIdDto,
        notificationEnabled: Boolean = true
    ) = optimisticLock {
        ownershipService.findByAuctionId(auctionId).map { ownership ->
            val updated = ownership.copy(auctions = ownership.auctions - auctionId)
            if (ownership != updated) {
                val saved = ownershipService.save(updated)
                if (notificationEnabled) {
                    notifyUpdate(saved)
                }
            } else {
                logger.info("Ownership [{}] not changed after auction deleted, event won't be published", ownership.id)
            }
        }.collect()
    }

    private suspend fun deleteOwnership(ownershipId: ShortOwnershipId): Boolean {
        val result = ownershipService.delete(ownershipId)
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
        order: OrderDto? = null,
        auction: AuctionDto? = null
    ) {
        val dto = ownershipService.enrichOwnership(short, ownership, listOfNotNull(order).associateBy { it.id }, listOfNotNull(auction).associateBy { it.id })
        val event = OwnershipUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            ownershipId = short.id.toDto(),
            ownership = dto
        )
        ownershipEventListeners.forEach { it.onEvent(event) }
    }
}
