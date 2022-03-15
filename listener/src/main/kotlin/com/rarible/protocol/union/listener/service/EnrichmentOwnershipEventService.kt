package com.rarible.protocol.union.listener.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.event.OutgoingOwnershipEventListener
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.model.getSellerOwnershipId
import com.rarible.protocol.union.core.model.ownershipId
import com.rarible.protocol.union.core.model.source
import com.rarible.protocol.union.core.service.AuctionContractService
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.enrichment.evaluator.OwnershipSourceComparator
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.BestOrderService
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityService
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
    private val enrichmentActivityService: EnrichmentActivityService,
    private val ownershipEventListeners: List<OutgoingOwnershipEventListener>,
    private val bestOrderService: BestOrderService,
    private val auctionContractService: AuctionContractService,
    private val reconciliationEventService: ReconciliationEventService
) {

    private val logger = LoggerFactory.getLogger(EnrichmentOwnershipEventService::class.java)

    suspend fun onOwnershipUpdated(ownership: UnionOwnership) {
        val existing = enrichmentOwnershipService.getOrEmpty(ShortOwnershipId(ownership.id))
        val event = buildUpdateEvent(existing, ownership)
        event?.let { sendUpdate(it) }
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
        val current = enrichmentOwnershipService.get(ownershipId)
        val exist = current != null
        val short = current ?: ShortOwnership.empty(ownershipId)

        val updated = bestOrderService.updateBestSellOrder(short, order)

        if (short != updated) {
            if (updated.isNotEmpty()) {
                saveAndNotify(updated, notificationEnabled, null, null, order)
                enrichmentItemEventService.onOwnershipUpdated(ownershipId, order, notificationEnabled)
            } else if (exist) {
                logger.info("Deleting Ownership [{}] without related bestSellOrder", ownershipId)
                cleanupAndNotify(updated, notificationEnabled, null, null, order)
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
            val dto = enrichmentOwnershipService.disguiseAuctionWithEnrichment(auction)
            dto?.let { sendUpdate(buildUpdateEvent(dto)) }
        } else {
            sendDelete(shortOwnershipId)
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
                buildUpdateEvent(existing, ownership, auction, null)?.let { sendUpdate(it) }
            } else {
                // There is no sense to attach inactive auctions to Ownerships
                buildUpdateEvent(existing, ownership, null, null)?.let { sendUpdate(it) }
            }
        } else if (auction.status == AuctionStatusDto.ACTIVE) {
            logger.info(
                "Received Auction [{}] event with status {}, existing seller Ownership [{}] not found - disguise event",
                auction.id, auction.status, ownershipId
            )
            // Send disguised ownership with updated auction
            enrichmentOwnershipService.disguiseAuctionWithEnrichment(auction)?.let {
                sendUpdate(buildUpdateEvent(it))
            }
        } else if (auction.status == AuctionStatusDto.FINISHED) {
            // If auction is finished and there is still no related ownership,
            // it means transfer from seller to buyer has been executed, we need to notify market about deletion
            logger.info(
                "Received Auction [{}] event with status {}, existing seller Ownership [{}] not found - deleting it",
                auction.id, auction.status, ownershipId
            )
            deleteOwnership(ownershipId)
            sendDelete(ownershipId)
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

    suspend fun onActivity(
        activity: ActivityDto,
        ownership: UnionOwnership? = null,
        notificationEnabled: Boolean = true
    ) {
        val source = activity.source() ?: return
        val ownershipId = activity.ownershipId() ?: return

        optimisticLock {
            val existing = enrichmentOwnershipService.getOrEmpty(ShortOwnershipId(ownershipId))
            val newSource = if (activity.reverted == true) {
                // We should re-evaluate source only if received activity has the same source
                if (source == existing.source) {
                    logger.info("Reverting Activity source {} for Ownership [{}]", source, ownershipId)
                    enrichmentActivityService.getOwnershipSource(ownershipId)
                } else {
                    existing.source
                }
            } else {
                OwnershipSourceComparator.getPreferred(existing.source, source)
            }

            if (newSource == existing.source) {
                logger.info("Ownership [{}] not changed after Activity event [{}]", ownershipId, activity.id)
            } else {
                logger.info(
                    "Ownership [{}] source changed on Activity event [{}]: {} -> {}",
                    ownershipId, activity.id, existing.source, newSource
                )
                saveAndNotify(existing.copy(source = newSource), notificationEnabled, ownership)
            }
        }
    }

    private suspend fun deleteOwnership(ownershipId: ShortOwnershipId): Boolean {
        val result = enrichmentOwnershipService.delete(ownershipId)
        return result != null && result.deletedCount > 0
    }

    private suspend fun saveAndNotify(
        updated: ShortOwnership,
        notificationEnabled: Boolean,
        ownership: UnionOwnership? = null,
        auction: AuctionDto? = null,
        order: OrderDto? = null
    ) {
        if (!notificationEnabled) {
            enrichmentOwnershipService.save(updated)
            return
        }

        val event = buildUpdateEvent(updated, ownership, auction, order)
        enrichmentOwnershipService.save(updated)
        event?.let { sendUpdate(event) }
    }

    private suspend fun cleanupAndNotify(
        updated: ShortOwnership,
        notificationEnabled: Boolean,
        ownership: UnionOwnership? = null,
        auction: AuctionDto? = null,
        order: OrderDto? = null
    ) {
        if (!notificationEnabled) {
            enrichmentOwnershipService.delete(updated.id)
            return
        }

        val event = buildUpdateEvent(updated, ownership, auction, order)
        enrichmentOwnershipService.delete(updated.id)
        event?.let { sendUpdate(event) }
    }

    private suspend fun buildUpdateEvent(
        short: ShortOwnership,
        ownership: UnionOwnership? = null,
        auction: AuctionDto? = null,
        order: OrderDto? = null
    ): OwnershipUpdateEventDto? {
        val isAuctionOwnership = (auctionContractService.isAuctionContract(short.blockchain, short.owner))
        if (isAuctionOwnership) {
            // We should skip auction ownerships
            return null
        }

        val dto = coroutineScope {
            val auctionDeferred = async { auction ?: enrichmentAuctionService.fetchOwnershipAuction(short.id) }
            val orders = listOfNotNull(order).associateBy { it.id }
            val enriched = enrichmentOwnershipService.enrichOwnership(short, ownership, orders)
            enrichmentOwnershipService.mergeWithAuction(enriched, auctionDeferred.await())
        }

        return buildUpdateEvent(dto)
    }

    private suspend fun buildUpdateEvent(dto: OwnershipDto): OwnershipUpdateEventDto {
        return OwnershipUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            ownershipId = dto.id,
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
