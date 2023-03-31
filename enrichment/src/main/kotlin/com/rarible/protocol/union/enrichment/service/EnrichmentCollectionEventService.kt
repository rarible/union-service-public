package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.event.OutgoingCollectionEventListener
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionChangeEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.core.model.UnionEventTimeMarks
import com.rarible.protocol.union.core.service.OriginService
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.model.ShortCollection
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.enrichment.validator.EntityValidator
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EnrichmentCollectionEventService(
    private val itemEventListeners: List<OutgoingCollectionEventListener>,
    private val enrichmentCollectionService: EnrichmentCollectionService,
    private val reconciliationEventService: ReconciliationEventService,
    private val bestOrderService: BestOrderService,
    private val originService: OriginService
) {

    private val logger = LoggerFactory.getLogger(EnrichmentCollectionEventService::class.java)

    suspend fun onCollectionChanged(event: UnionCollectionChangeEvent) {
        val collectionId = event.collectionId
        val existing = enrichmentCollectionService.getOrEmpty(ShortCollectionId(collectionId))
        val updateEvent = buildUpdateEvent(
            short = existing,
            eventTimeMarks = event.eventTimeMarks
        )
        sendUpdate(updateEvent)
    }

    suspend fun onCollectionUpdate(update: UnionCollectionUpdateEvent) {
        val collection = update.collection
        val existing = enrichmentCollectionService.getOrCreateWithLastUpdatedAtUpdate(ShortCollectionId(collection.id))
        val updateEvent = buildUpdateEvent(
            short = existing,
            collection = collection,
            eventTimeMarks = update.eventTimeMarks
        )
        sendUpdate(updateEvent)
    }

    suspend fun onCollectionBestSellOrderUpdate(
        collectionId: CollectionIdDto,
        order: OrderDto,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean
    ) = coroutineScope {
        updateCollection(
            ShortCollectionId(collectionId),
            order,
            eventTimeMarks,
            notificationEnabled
        ) { collection ->
            val origins = originService.getOrigins(collectionId)
            bestOrderService.updateBestSellOrder(collection, order, origins)
        }
    }

    suspend fun onCollectionBestBidOrderUpdate(
        collectionId: CollectionIdDto,
        order: OrderDto,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean
    ) = coroutineScope {
        updateCollection(
            ShortCollectionId(collectionId),
            order,
            eventTimeMarks,
            notificationEnabled
        ) { collection ->
            val origins = originService.getOrigins(collectionId)
            bestOrderService.updateBestBidOrder(collection, order, origins)
        }
    }

    suspend fun recalculateBestOrders(
        collection: ShortCollection,
        eventTimeMarks: UnionEventTimeMarks?
    ): Boolean {
        val updated = bestOrderService.updateBestOrders(collection)
        if (updated != collection) {
            logger.info(
                "Collection BestSellOrder updated ([{}] -> [{}]), BestBidOrder updated ([{}] -> [{}]) due to currency rate changed",
                collection.bestSellOrder?.dtoId, updated.bestSellOrder?.dtoId,
                collection.bestBidOrder?.dtoId, updated.bestBidOrder?.dtoId
            )
            saveAndNotify(
                updated = updated,
                notificationEnabled = true,
                eventTimeMarks = eventTimeMarks
            )
            return true
        }
        return false
    }

    private suspend fun updateCollection(
        itemId: ShortCollectionId,
        order: OrderDto,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean,
        orderUpdateAction: suspend (item: ShortCollection) -> ShortCollection
    ) = optimisticLock {
        val (short, updated, exist) = update(itemId, orderUpdateAction)
        if (short != updated && (exist || updated.isNotEmpty())) {
            saveAndNotify(
                updated = updated,
                notificationEnabled = notificationEnabled,
                eventTimeMarks = eventTimeMarks,
                order = order
            )
            logger.info("Saved Collection [{}] after Order event [{}]", itemId, order.id)
        } else {
            logger.info(
                "Collection [{}] not changed after Order event [{}], event won't be published", itemId, order.id
            )
        }
    }

    private suspend fun update(
        collectionId: ShortCollectionId,
        action: suspend (collection: ShortCollection) -> ShortCollection
    ): Triple<ShortCollection?, ShortCollection, Boolean> {
        val current = enrichmentCollectionService.get(collectionId)
        val exist = current != null
        val short = current ?: ShortCollection.empty(collectionId)
        return Triple(current, action(short), exist)
    }

    private suspend fun saveAndNotify(
        updated: ShortCollection,
        notificationEnabled: Boolean,
        collection: UnionCollection? = null,
        order: OrderDto? = null,
        eventTimeMarks: UnionEventTimeMarks?
    ) {
        if (!notificationEnabled) {
            enrichmentCollectionService.save(updated)
            return
        }

        val event = buildUpdateEvent(updated, collection, order, eventTimeMarks)
        enrichmentCollectionService.save(updated)
        sendUpdate(event)
    }

    private suspend fun buildUpdateEvent(
        short: ShortCollection,
        collection: UnionCollection? = null,
        order: OrderDto? = null,
        eventTimeMarks: UnionEventTimeMarks?
    ): CollectionUpdateEventDto {
        val dto = enrichmentCollectionService.enrichCollection(
            shortCollection = short,
            collection = collection,
            orders = listOfNotNull(order).associateBy { it.id },
            metaPipeline = CollectionMetaPipeline.EVENT
        )

        return CollectionUpdateEventDto(
            collectionId = dto.id,
            collection = dto,
            eventId = UUID.randomUUID().toString(),
            eventTimeMarks = eventTimeMarks?.addOut()?.toDto()
        )
    }

    private suspend fun sendUpdate(event: CollectionUpdateEventDto) {
        // If collection in corrupted state, we will try to reconcile it instead of sending corrupted
        // data to the customers
        if (!EntityValidator.isValid(event.collection)) {
            reconciliationEventService.onCorruptedCollection(event.collection.id)
        } else {
            itemEventListeners.forEach { it.onEvent(event) }
        }
    }
}
