package com.rarible.protocol.union.listener.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.event.OutgoingItemEventListener
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.ItemDeleteEventDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.model.ItemSellStats
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.BestOrderService
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class EnrichmentItemEventService(
    private val itemService: EnrichmentItemService,
    private val ownershipService: EnrichmentOwnershipService,
    private val itemEventListeners: List<OutgoingItemEventListener>,
    private val bestOrderService: BestOrderService
) {

    private val logger = LoggerFactory.getLogger(EnrichmentItemEventService::class.java)

    // If ownership was updated, we need to recalculate totalStock/sellers for related item,
    // also, we can specify here Order which triggered this update - ItemService
    // can use this full Order to avoid unnecessary getOrderById calls
    suspend fun onOwnershipUpdated(ownershipId: ShortOwnershipId, order: OrderDto?) {
        val itemId = ShortItemId(ownershipId.blockchain, ownershipId.token, ownershipId.tokenId)
        optimisticLock {
            val item = itemService.get(itemId)
            if (item == null) {
                logger.debug(
                    "Item [{}] not found in DB, skipping sell stats update on Ownership event: [{}]",
                    itemId, ownershipId
                )
            } else {
                val refreshedSellStats = ownershipService.getItemSellStats(itemId)
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
                    val saved = itemService.save(updatedItem)
                    notifyUpdate(saved, null, order)
                } else {
                    logger.debug(
                        "Sell stats of Item [{}] are the same as before Ownership event [{}], skipping update",
                        itemId, ownershipId
                    )
                }
            }
        }
    }

    suspend fun onItemUpdated(item: UnionItem) {
        val received = ShortItemConverter.convert(item)
        val existing = itemService.getOrEmpty(received.id)
        notifyUpdate(existing, item)
    }

    suspend fun recalculateBestOrders(item: ShortItem): Boolean {
        val updated = bestOrderService.updateBestOrders(item)
        if (updated != item) {
            logger.info(
                "Item BestSellOrder updated ([{}] -> [{}]), BestBidOrder updated ([{}] -> [{}]) due to currency rate changed",
                item.bestSellOrder?.dtoId, updated.bestSellOrder?.dtoId,
                item.bestBidOrder?.dtoId, updated.bestBidOrder?.dtoId
            )
            val saved = itemService.save(updated)
            notifyUpdate(saved, null, null)
            return true
        }
        return false
    }

    suspend fun onItemBestSellOrderUpdated(itemId: ShortItemId, order: OrderDto) {
        updateOrder(itemId, order) { item -> bestOrderService.updateBestSellOrder(item, order) }
    }

    suspend fun onItemBestBidOrderUpdated(itemId: ShortItemId, order: OrderDto) {
        updateOrder(itemId, order) { item -> bestOrderService.updateBestBidOrder(item, order) }
    }

    private suspend fun updateOrder(
        itemId: ShortItemId,
        order: OrderDto,
        orderUpdateAction: suspend (item: ShortItem) -> ShortItem
    ) = optimisticLock {
        val current = itemService.get(itemId)
        val exist = current != null
        val short = current ?: ShortItem.empty(itemId)

        val updated = orderUpdateAction(short)

        if (short != updated) {
            if (updated.isNotEmpty()) {
                val saved = itemService.save(updated)
                notifyUpdate(saved, null, order)
            } else if (exist) {
                itemService.delete(itemId)
                logger.info("Deleted Item [{}] without enrichment data", itemId)
                notifyUpdate(updated, null, order)
            }
        } else {
            logger.info("Item [{}] not changed after order updated, event won't be published", itemId)
        }
    }

    suspend fun onItemDeleted(itemId: ItemIdDto) {
        val shortItemId = ShortItemId(itemId)
        val deleted = deleteItem(shortItemId)
        notifyDelete(shortItemId)
        if (deleted) {
            logger.info("Item [{}] deleted (removed from NFT-Indexer)", shortItemId)
        }
    }

    private suspend fun deleteItem(itemId: ShortItemId): Boolean {
        val result = itemService.delete(itemId)
        return result != null && result.deletedCount > 0
    }

    private suspend fun notifyDelete(itemId: ShortItemId) {
        val event = ItemDeleteEventDto(
            itemId = itemId.toDto(),
            eventId = UUID.randomUUID().toString()
        )
        itemEventListeners.forEach { it.onEvent(event) }
    }

    // Potentially we could have updated Order here (no matter - bid/sell) and when we need to fetch
    // full version of the order, we can use this already fetched Order if it has same ID (hash)
    private suspend fun notifyUpdate(
        short: ShortItem,
        item: UnionItem? = null,
        order: OrderDto? = null
    ) {
        val dto = itemService.enrichItem(short, item, listOfNotNull(order).associateBy { it.id })
        val event = ItemUpdateEventDto(
            itemId = dto.id,
            item = dto,
            eventId = UUID.randomUUID().toString()
        )
        itemEventListeners.forEach { it.onEvent(event) }
    }
}
