package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.event.OutgoingEventListener
import com.rarible.protocol.union.core.model.PoolItemAction
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionEventTimeMarks
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.core.model.getSellerOwnershipId
import com.rarible.protocol.union.core.model.ownershipId
import com.rarible.protocol.union.core.model.source
import com.rarible.protocol.union.core.service.AuctionContractService
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipSourceDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.enrichment.evaluator.OwnershipSourceComparator
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.util.setStatusByAction
import com.rarible.protocol.union.enrichment.validator.OwnershipValidator
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class EnrichmentOwnershipEventService(
    private val enrichmentOwnershipService: EnrichmentOwnershipService,
    private val enrichmentItemService: EnrichmentItemService,
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentAuctionService: EnrichmentAuctionService,
    private val enrichmentActivityService: EnrichmentActivityService,
    private val ownershipEventListeners: List<OutgoingEventListener<OwnershipEventDto>>,
    private val bestOrderService: BestOrderService,
    private val auctionContractService: AuctionContractService,
    private val reconciliationEventService: ReconciliationEventService
) {

    private val logger = LoggerFactory.getLogger(EnrichmentOwnershipEventService::class.java)

    suspend fun onOwnershipUpdated(event: UnionOwnershipUpdateEvent) {
        val ownership = event.ownership
        val existing = enrichmentOwnershipService.getOrCreateWithLastUpdatedAtUpdate(ShortOwnershipId(ownership.id))
        buildUpdateEvent(
            short = existing,
            ownership = ownership,
            eventTimeMarks = event.eventTimeMarks
        )?.let { sendUpdate(it) }
    }

    suspend fun recalculateBestOrders(ownership: ShortOwnership, eventTimeMarks: UnionEventTimeMarks?): Boolean {
        val updated = bestOrderService.updateBestOrders(ownership)
        if (ownership.bestSellOrder != updated.bestSellOrder) {
            logger.info(
                "Ownership BestSellOrder updated ([{}] -> [{}]) due to currency rate changed",
                ownership.bestSellOrder?.dtoId, updated.bestSellOrder?.dtoId
            )

            saveAndNotify(
                updated = updated,
                notificationEnabled = false,
                eventTimeMarks = eventTimeMarks
            )
            enrichmentItemEventService.onOwnershipUpdated(
                ownershipId = ownership.id,
                order = null,
                eventTimeMarks = eventTimeMarks
            )
            return true
        }
        return false
    }

    suspend fun onPoolOrderUpdated(
        ownershipId: ShortOwnershipId,
        order: OrderDto,
        action: PoolItemAction,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean = true
    ) {
        val hackedOrder = order.setStatusByAction(action)
        return onOwnershipBestSellOrderUpdated(ownershipId, hackedOrder, eventTimeMarks, notificationEnabled)
    }

    suspend fun onOwnershipBestSellOrderUpdated(
        ownershipId: ShortOwnershipId,
        order: OrderDto,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean = true
    ) = optimisticLock {
        val current = enrichmentOwnershipService.get(ownershipId)
        val exist = current != null
        val short = current ?: ShortOwnership.empty(ownershipId)

        val origins = enrichmentItemService.getItemOrigins(ownershipId.getItemId())
        val updated = bestOrderService.updateBestSellOrder(short, order, origins)

        if (short != updated && (exist || updated.isNotEmpty())) {
            saveAndNotify(updated, notificationEnabled, null, null, order, eventTimeMarks)
            enrichmentItemEventService.onOwnershipUpdated(ownershipId, order, eventTimeMarks, notificationEnabled)
        } else {
            logger.info("Ownership [{}] not changed after order updated, event won't be published", ownershipId)
        }
    }

    suspend fun onOwnershipDeleted(event: UnionOwnershipDeleteEvent) = coroutineScope {
        val ownershipId = event.ownershipId
        val shortOwnershipId = ShortOwnershipId(ownershipId)
        val ownershipAuctionDeferred = async { enrichmentAuctionService.fetchOwnershipAuction(shortOwnershipId) }

        logger.debug("Deleting Ownership [{}] since it was removed from NFT-Indexer", shortOwnershipId)
        val deleted = deleteOwnership(shortOwnershipId)
        val auction = ownershipAuctionDeferred.await()
        if (auction != null) {
            // In case such ownership is belongs to auction, we have to do not send delete event
            val dto = enrichmentOwnershipService.disguiseAuctionWithEnrichment(auction)
            dto?.let { sendUpdate(buildUpdateEvent(dto, event.eventTimeMarks)) }
        } else {
            sendDelete(buildDeleteEvent(ShortOwnershipId(ownershipId), event.eventTimeMarks))
            if (deleted) {
                logger.info(
                    "Ownership [{}] deleted (removed from NFT-Indexer), refreshing sell stats",
                    shortOwnershipId
                )
                enrichmentItemEventService.onOwnershipUpdated(
                    ownershipId = shortOwnershipId,
                    order = null,
                    eventTimeMarks = event.eventTimeMarks
                )
            }
        }

        Unit
    }

    @Deprecated("Not used")
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
                buildUpdateEvent(existing, ownership, auction, null, null)?.let { sendUpdate(it) }
            } else {
                // There is no sense to attach inactive auctions to Ownerships
                buildUpdateEvent(existing, ownership, null, null, null)?.let { sendUpdate(it) }
            }
        } else if (auction.status == AuctionStatusDto.ACTIVE) {
            logger.info(
                "Received Auction [{}] event with status {}, existing seller Ownership [{}] not found - disguise event",
                auction.id, auction.status, ownershipId
            )
            // Send disguised ownership with updated auction
            enrichmentOwnershipService.disguiseAuctionWithEnrichment(auction)?.let {
                sendUpdate(buildUpdateEvent(it, null))
            }
        } else if (auction.status == AuctionStatusDto.FINISHED) {
            // If auction is finished and there is still no related ownership,
            // it means transfer from seller to buyer has been executed, we need to notify market about deletion
            logger.info(
                "Received Auction [{}] event with status {}, existing seller Ownership [{}] not found - deleting it",
                auction.id, auction.status, ownershipId
            )
            deleteOwnership(ownershipId)
            sendDelete(buildDeleteEvent(ownershipId, null))
        }
        // If status = CANCELLED, nothing to do here, we'll receive updated via OwnershipEvents
    }

    @Deprecated("Not used")
    suspend fun onAuctionDeleted(auction: AuctionDto) {
        val ownershipId = ShortOwnershipId(auction.getSellerOwnershipId())
        enrichmentOwnershipService.fetchOrNull(ownershipId)?.let {
            // Consider as regular update, auction won't be present in event since it is deleted
            onOwnershipUpdated(UnionOwnershipUpdateEvent(it, null))
        }
    }

    @Deprecated("keep UnionActivity only")
    suspend fun onActivityLegacy(
        activity: ActivityDto,
        ownership: UnionOwnership? = null,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean = true
    ) {
        val source = activity.source() ?: return
        val ownershipId = activity.ownershipId() ?: return
        onActivity(
            id = activity.id,
            reverted = activity.reverted,
            source = source,
            ownershipId = ownershipId,
            ownership = ownership,
            eventTimeMarks = eventTimeMarks,
            notificationEnabled = notificationEnabled
        )
    }

    suspend fun onActivity(
        activity: UnionActivity,
        ownership: UnionOwnership? = null,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean = true
    ) {
        val source = activity.source() ?: return
        val ownershipId = activity.ownershipId() ?: return
        onActivity(
            id = activity.id,
            reverted = activity.reverted,
            source = source,
            ownershipId = ownershipId,
            ownership = ownership,
            eventTimeMarks = eventTimeMarks,
            notificationEnabled = notificationEnabled
        )
    }

    private suspend fun onActivity(
        id: ActivityIdDto,
        reverted: Boolean?,
        source: OwnershipSourceDto,
        ownershipId: OwnershipIdDto,
        ownership: UnionOwnership? = null,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean = true
    ) {
        optimisticLock {
            val existing = enrichmentOwnershipService.getOrEmpty(ShortOwnershipId(ownershipId))
            val newSource = if (reverted == true) {
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
                logger.info("Ownership [{}] not changed after Activity event [{}]", ownershipId, id)
            } else {
                logger.info(
                    "Ownership [{}] source changed on Activity event [{}]: {} -> {}",
                    ownershipId, id, existing.source, newSource
                )
                saveAndNotify(
                    updated = existing.copy(source = newSource),
                    notificationEnabled = notificationEnabled,
                    ownership = ownership,
                    eventTimeMarks = eventTimeMarks
                )
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
        order: OrderDto? = null,
        eventTimeMarks: UnionEventTimeMarks?
    ) {
        if (!notificationEnabled) {
            enrichmentOwnershipService.save(updated)
            return
        }

        val saved = enrichmentOwnershipService.save(updated)
        val event = buildUpdateEvent(saved, ownership, auction, order, eventTimeMarks)
        event?.let { sendUpdate(event) }
    }

    private suspend fun buildUpdateEvent(
        short: ShortOwnership,
        ownership: UnionOwnership? = null,
        auction: AuctionDto? = null,
        order: OrderDto? = null,
        eventTimeMarks: UnionEventTimeMarks?
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

        return buildUpdateEvent(dto, eventTimeMarks)
    }

    private suspend fun buildUpdateEvent(
        dto: OwnershipDto,
        eventTimeMarks: UnionEventTimeMarks?
    ): OwnershipUpdateEventDto {
        return OwnershipUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            ownershipId = dto.id,
            ownership = dto,
            eventTimeMarks = eventTimeMarks?.addOut()?.toDto()
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

    private suspend fun buildDeleteEvent(
        ownershipId: ShortOwnershipId,
        eventTimeMarks: UnionEventTimeMarks?
    ): OwnershipDeleteEventDto {
        return OwnershipDeleteEventDto(
            eventId = UUID.randomUUID().toString(),
            ownershipId = ownershipId.toDto(),
            eventTimeMarks = eventTimeMarks?.addOut()?.toDto()
        )
    }

    private suspend fun sendDelete(event: OwnershipDeleteEventDto) {
        ownershipEventListeners.forEach { it.onEvent(event) }
    }
}
