package com.rarible.protocol.union.listener.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.event.OutgoingEventListener
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.getItemId
import com.rarible.protocol.union.core.model.itemId
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.ItemDeleteEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.converter.ItemLastSaleConverter
import com.rarible.protocol.union.enrichment.model.ItemSellStats
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.BestOrderService
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityService
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.validator.EntityValidator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class EnrichmentItemEventService(
    private val enrichmentItemService: EnrichmentItemService,
    private val enrichmentOwnershipService: EnrichmentOwnershipService,
    private val enrichmentActivityService: EnrichmentActivityService,
    private val itemEventListeners: List<OutgoingEventListener<ItemEventDto>>,
    private val bestOrderService: BestOrderService,
    private val reconciliationEventService: ReconciliationEventService
) {

    private val logger = LoggerFactory.getLogger(EnrichmentItemEventService::class.java)

    // If ownership was updated, we need to recalculate totalStock/sellers for related item,
    // also, we can specify here Order which triggered this update - ItemService
    // can use this full Order to avoid unnecessary getOrderById calls
    suspend fun onOwnershipUpdated(
        ownershipId: ShortOwnershipId,
        order: OrderDto?,
        notificationEnabled: Boolean = true
    ) {
        val itemId = ShortItemId(ownershipId.blockchain, ownershipId.itemId)
        optimisticLock {
            val item = enrichmentItemService.get(itemId)
            if (item == null) {
                logger.debug(
                    "Item [{}] not found in DB, skipping sell stats update on Ownership event: [{}]",
                    itemId, ownershipId
                )
            } else {
                val refreshedSellStats = enrichmentOwnershipService.getItemSellStats(itemId)
                val currentSellStats = ItemSellStats(item.sellers, item.totalStock)
                if (refreshedSellStats != currentSellStats) {
                    val updatedItem = item.copy(
                        sellers = refreshedSellStats.sellers,
                        totalStock = refreshedSellStats.totalStock
                    )
                    logger.info(
                        "Updating Item [{}] with new sell stats, was [{}] , now: [{}]",
                        itemId, currentSellStats, refreshedSellStats
                    )
                    saveAndNotify(updated = updatedItem, notificationEnabled = notificationEnabled, order = order)
                } else {
                    logger.debug(
                        "Sell stats of Item [{}] are the same as before Ownership event [{}], skipping update",
                        itemId, ownershipId
                    )
                }
            }
        }
    }

    suspend fun onActivity(activity: ActivityDto, item: UnionItem? = null, notificationEnabled: Boolean = true) {
        val lastSale = ItemLastSaleConverter.convert(activity) ?: return
        val itemId = activity.itemId() ?: return

        optimisticLock {
            val existing = enrichmentItemService.getOrEmpty(ShortItemId(itemId))
            val currentLastSale = existing.lastSale

            val newLastSale = if (activity.reverted == true) {
                // We should re-evaluate last sale only if received activity has the same sale data
                if (lastSale == currentLastSale) {
                    logger.info("Reverting Activity LastSale {} for Item [{}], reverting it", lastSale, itemId)
                    enrichmentActivityService.getItemLastSale(itemId)
                } else {
                    currentLastSale
                }
            } else {
                if (currentLastSale == null || currentLastSale.date.isBefore(lastSale.date)) {
                    lastSale
                } else {
                    currentLastSale
                }
            }

            if (newLastSale == currentLastSale) {
                logger.info("Item [{}] not changed after Activity event [{}]", itemId, activity.id)
            } else {
                logger.info(
                    "Item [{}] LastSale changed on Activity event [{}]: {} -> {}",
                    itemId, activity.id, currentLastSale, newLastSale
                )
                saveAndNotify(existing.copy(lastSale = newLastSale), notificationEnabled, item)
            }
        }
    }

    suspend fun onItemUpdated(item: UnionItem) {
        val existing = enrichmentItemService.getOrEmpty(ShortItemId(item.id))
        val updateEvent = buildUpdateEvent(short = existing, item = item)
        sendUpdate(updateEvent)
    }

    suspend fun recalculateBestOrders(item: ShortItem): Boolean {
        val updated = bestOrderService.updateBestOrders(item)
        if (updated != item) {
            logger.info(
                "Item BestSellOrder updated ([{}] -> [{}]), BestBidOrder updated ([{}] -> [{}]) due to currency rate changed",
                item.bestSellOrder?.dtoId, updated.bestSellOrder?.dtoId,
                item.bestBidOrder?.dtoId, updated.bestBidOrder?.dtoId
            )
            saveAndNotify(updated, true)
            return true
        }
        return false
    }

    suspend fun onItemBestSellOrderUpdated(itemId: ShortItemId, order: OrderDto, notificationEnabled: Boolean = true) {
        updateOrder(itemId, order, notificationEnabled) { item ->
            val origins = enrichmentItemService.getItemOrigins(itemId)
            bestOrderService.updateBestSellOrder(item, order, origins)
        }
    }

    suspend fun onItemBestBidOrderUpdated(itemId: ShortItemId, order: OrderDto, notificationEnabled: Boolean = true) {
        updateOrder(itemId, order, notificationEnabled) { item ->
            val origins = enrichmentItemService.getItemOrigins(itemId)
            bestOrderService.updateBestBidOrder(item, order, origins)
        }
    }

    suspend fun onAuctionUpdated(auction: AuctionDto, notificationEnabled: Boolean = true) {
        updateAuction(auction, notificationEnabled) {
            if (auction.status == AuctionStatusDto.ACTIVE) {
                it.copy(auctions = it.auctions + auction.id)
            } else {
                it.copy(auctions = it.auctions - auction.id)
            }
        }
    }

    suspend fun onAuctionDeleted(auction: AuctionDto, notificationEnabled: Boolean = true) {
        updateAuction(auction, notificationEnabled) { it.copy(auctions = it.auctions - auction.id) }
    }

    private suspend fun updateOrder(
        itemId: ShortItemId,
        order: OrderDto,
        notificationEnabled: Boolean,
        orderUpdateAction: suspend (item: ShortItem) -> ShortItem
    ) = optimisticLock {
        val (short, updated, exist) = update(itemId, orderUpdateAction)
        if (short != updated) {
            if (updated.isNotEmpty()) {
                saveAndNotify(updated = updated, notificationEnabled = notificationEnabled, order = order)
                logger.info("Saved Item [{}] after Order event [{}]", itemId, order.id)
            } else if (exist) {
                cleanupAndNotify(updated = updated, notificationEnabled = notificationEnabled, order = order)
                logger.info("Deleted Item [{}] without enrichment data", itemId)
            }
        } else {
            logger.info("Item [{}] not changed after Order event [{}], event won't be published", itemId, order.id)
        }
    }

    private suspend fun updateAuction(
        auction: AuctionDto,
        notificationEnabled: Boolean,
        updateAction: suspend (item: ShortItem) -> ShortItem
    ) = optimisticLock {
        val itemId = ShortItemId(auction.getItemId())
        val (short, updated, exist) = update(itemId, updateAction)
        if (short != updated) {
            if (updated.isNotEmpty()) {
                saveAndNotify(updated = updated, notificationEnabled = notificationEnabled, auction = auction)
                logger.info("Saved Item [{}] after Auction event [{}]", itemId, auction.auctionId)
            } else if (exist) {
                cleanupAndNotify(updated = updated, notificationEnabled = notificationEnabled, auction = auction)
                logger.info("Deleted Item [{}] without enrichment data", itemId)
            }
        } else {
            logger.info("Item [{}] not changed after Auction event [{}], event won't be published", itemId, auction.id)
        }
    }

    suspend fun onItemDeleted(itemId: ItemIdDto) {
        val shortItemId = ShortItemId(itemId)
        val deleted = deleteItem(shortItemId)
        sendDelete(shortItemId)
        if (deleted) {
            logger.info("Item [{}] deleted (removed from NFT-Indexer)", shortItemId)
        }
    }

    private suspend fun deleteItem(itemId: ShortItemId): Boolean {
        val result = enrichmentItemService.delete(itemId)
        return result != null && result.deletedCount > 0
    }

    private suspend fun update(
        itemId: ShortItemId,
        action: suspend (item: ShortItem) -> ShortItem
    ): Triple<ShortItem?, ShortItem, Boolean> {
        val current = enrichmentItemService.get(itemId)
        val exist = current != null
        val short = current ?: ShortItem.empty(itemId)
        return Triple(current, action(short), exist)
    }

    // Potentially we could have updated Order here (no matter - bid/sell) and when we need to fetch
    // full version of the order, we can use this already fetched Order if it has same ID (hash)
    private suspend fun saveAndNotify(
        updated: ShortItem,
        notificationEnabled: Boolean,
        item: UnionItem? = null,
        order: OrderDto? = null,
        auction: AuctionDto? = null
    ) {
        if (!notificationEnabled) {
            enrichmentItemService.save(updated)
            return
        }

        val event = buildUpdateEvent(updated, item, order, auction)
        enrichmentItemService.save(updated)
        sendUpdate(event)
    }

    private suspend fun cleanupAndNotify(
        updated: ShortItem,
        notificationEnabled: Boolean,
        item: UnionItem? = null,
        order: OrderDto? = null,
        auction: AuctionDto? = null
    ) {
        if (!notificationEnabled) {
            enrichmentItemService.delete(updated.id)
            return
        }

        val event = buildUpdateEvent(updated, item, order, auction)
        enrichmentItemService.delete(updated.id)
        sendUpdate(event)
    }

    private suspend fun buildUpdateEvent(
        short: ShortItem,
        item: UnionItem? = null,
        order: OrderDto? = null,
        auction: AuctionDto? = null
    ): ItemUpdateEventDto {
        val dto = enrichmentItemService.enrichItem(
            shortItem = short,
            item = item,
            orders = listOfNotNull(order).associateBy { it.id },
            auctions = listOfNotNull(auction).associateBy { it.id }
        )

        return ItemUpdateEventDto(
            itemId = dto.id,
            item = dto,
            eventId = UUID.randomUUID().toString()
        )
    }

    private suspend fun sendUpdate(event: ItemUpdateEventDto) {
        // If item in corrupted state, we will try to reconcile it instead of sending corrupted
        // data to the customers
        if (!EntityValidator.isValid(event.item)) {
            reconciliationEventService.onCorruptedItem(event.item.id)
        } else {
            itemEventListeners.forEach { it.onEvent(event) }
        }
    }

    private suspend fun sendDelete(itemId: ShortItemId) {
        val event = ItemDeleteEventDto(
            itemId = itemId.toDto(),
            eventId = UUID.randomUUID().toString()
        )
        itemEventListeners.forEach { it.onEvent(event) }
    }
}
