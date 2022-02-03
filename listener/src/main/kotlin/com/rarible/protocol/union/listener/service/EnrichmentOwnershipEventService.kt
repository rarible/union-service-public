package com.rarible.protocol.union.listener.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.event.OutgoingOwnershipEventListener
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.model.getSellerOwnershipId
import com.rarible.protocol.union.core.service.AuctionContractService
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
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
    private val auctionContractService: AuctionContractService,
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
                    notifyUpdate(saved, null, null, order)
                }
                enrichmentItemEventService.onOwnershipUpdated(ownershipId, order, notificationEnabled)
            } else if (exist) {
                logger.info("Deleting Ownership [{}] without related bestSellOrder", ownershipId)
                enrichmentOwnershipService.delete(ownershipId)
                if (notificationEnabled) {
                    notifyUpdate(updated, null, null, order)
                }
                enrichmentItemEventService.onOwnershipUpdated(ownershipId, order, notificationEnabled)
            }
        } else {
            logger.info("Ownership [{}] not changed after order updated, event won't be published", ownershipId)
        }
    }

    suspend fun onOwnershipDeleted(ownershipId: OwnershipIdDto) = coroutineScope {
        val shortOwnershipId = ShortOwnershipId(ownershipId)
        val ownershipAuctionDeferred = async { enrichmentAuctionService.fetchOwnershipAuction(shortOwnershipId) }

        logger.debug("Deleting Ownership [{}] since it was removed from NFT-Indexer", shortOwnershipId)
        val deleted = deleteOwnership(shortOwnershipId)
        val auction = ownershipAuctionDeferred.await()
        if (auction != null) {
            // In case such ownership is belongs to auction, we have to do not send delete event
            val dto = enrichmentOwnershipService.disguiseAuction(auction)
            dto?.let { notifyUpdate(dto) }
        } else {
            notifyDelete(shortOwnershipId)
            if (deleted) {
                logger.info(
                    "Ownership [{}] deleted (removed from NFT-Indexer), refreshing sell stats",
                    shortOwnershipId
                )
                enrichmentItemEventService.onOwnershipUpdated(shortOwnershipId, null)
            }
        }

        Unit
    }

    suspend fun onAuctionUpdated(auction: AuctionDto) {
        val ownershipId = ShortOwnershipId(auction.getSellerOwnershipId())
        val ownership = enrichmentOwnershipService.fetchOrNull(ownershipId)
        val existing = enrichmentOwnershipService.getOrEmpty(ownershipId)

        if (ownership != null) {
            logger.info(
                "Received Auction [{}] event with status {}, existing seller Ownership [{}] found - merge them",
                auction.id, auction.status, ownershipId
            )
            if (auction.status == AuctionStatusDto.ACTIVE) {
                // Attaching new ACTIVE auction version to existing ownership
                notifyUpdate(existing, ownership, auction, null)
            } else {
                // There is no sense to attach inactive auctions to Ownerships
                notifyUpdate(existing, ownership, null, null)
            }
        } else if (auction.status == AuctionStatusDto.ACTIVE) {
            logger.info(
                "Received Auction [{}] event with status {}, existing seller Ownership [{}] not found - disguise event",
                auction.id, auction.status, ownershipId
            )
            // Send disguised ownership with updated auction
            enrichmentOwnershipService.disguiseAuction(auction)?.let {
                notifyUpdate(it)
            }
        } else if (auction.status == AuctionStatusDto.FINISHED) {
            // If auction is finished and there is still no related ownership,
            // it means transfer from seller to buyer has been executed, we need to notify market about deletion
            logger.info(
                "Received Auction [{}] event with status {}, existing seller Ownership [{}] not found - deleting it",
                auction.id, auction.status, ownershipId
            )
            deleteOwnership(ownershipId)
            notifyDelete(ownershipId)
        }
        // If status = CANCELLED, nothing to do here, we'll receive updated via OwnershipEvents
    }

    suspend fun onAuctionDeleted(auction: AuctionDto) {
        val ownershipId = ShortOwnershipId(auction.getSellerOwnershipId())
        enrichmentOwnershipService.fetchOrNull(ownershipId)?.let {
            // Consider as regular update, auction won't be present in event since it is deleted
            onOwnershipUpdated(it)
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
        auction: AuctionDto? = null,
        order: OrderDto? = null
    ) {
        val isAuctionOwnership = (auctionContractService.isAuctionContract(short.blockchain, short.owner))
        if (isAuctionOwnership) {
            // We should skip auction ownerships
            return
        }

        val dto = coroutineScope {
            val auctionDeferred = async { auction ?: enrichmentAuctionService.fetchOwnershipAuction(short.id) }
            val orders = listOfNotNull(order).associateBy { it.id }
            val enriched = enrichmentOwnershipService.enrichOwnership(short, ownership, orders)
            enrichmentOwnershipService.mergeWithAuction(enriched, auctionDeferred.await())
        }

        notifyUpdate(dto)
    }

    private suspend fun notifyUpdate(dto: OwnershipDto) {
        val event = OwnershipUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            ownershipId = dto.id,
            ownership = dto
        )

        if (!OwnershipValidator.isValid(dto)) {
            reconciliationEventService.onCorruptedOwnership(dto.id)
        }
        ownershipEventListeners.forEach { it.onEvent(event) }
    }
}
