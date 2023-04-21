package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.event.OutgoingCollectionEventListener
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionChangeEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.core.model.UnionEventTimeMarks
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.service.OriginService
import com.rarible.protocol.union.core.service.ReconciliationEventService
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
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

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun onCollectionChanged(event: UnionCollectionChangeEvent) {
        val collectionId = event.collectionId
        val existing = enrichmentCollectionService.getOrFetch(EnrichmentCollectionId(collectionId))
        val updateEvent = buildUpdateEvent(
            enrichmentCollection = existing,
            eventTimeMarks = event.eventTimeMarks
        )
        sendUpdate(updateEvent)
    }

    suspend fun onCollectionUpdate(update: UnionCollectionUpdateEvent) {
        val collection = update.collection
        val existing = optimisticLock { enrichmentCollectionService.update(collection) }
        val updateEvent = buildUpdateEvent(
            enrichmentCollection = existing,
            unionCollection = collection,
            eventTimeMarks = update.eventTimeMarks
        )
        sendUpdate(updateEvent)
    }

    suspend fun onCollectionBestSellOrderUpdate(
        collectionId: CollectionIdDto,
        order: UnionOrder,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean
    ) = coroutineScope {
        updateCollection(
            EnrichmentCollectionId(collectionId),
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
        order: UnionOrder,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean
    ) = coroutineScope {
        updateCollection(
            EnrichmentCollectionId(collectionId),
            order,
            eventTimeMarks,
            notificationEnabled
        ) { collection ->
            val origins = originService.getOrigins(collectionId)
            bestOrderService.updateBestBidOrder(collection, order, origins)
        }
    }

    suspend fun recalculateBestOrders(
        collection: EnrichmentCollection,
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
        collectionId: EnrichmentCollectionId,
        order: UnionOrder,
        eventTimeMarks: UnionEventTimeMarks?,
        notificationEnabled: Boolean,
        orderUpdateAction: suspend (collection: EnrichmentCollection) -> EnrichmentCollection
    ) = optimisticLock {
        val current = enrichmentCollectionService.getOrFetch(collectionId)
        val updated = orderUpdateAction(current)

        // Notify if something changed OR just created
        if (current != updated || current.version == null) {
            saveAndNotify(
                updated = updated,
                notificationEnabled = notificationEnabled,
                eventTimeMarks = eventTimeMarks,
                order = order
            )
            logger.info("Saved Collection [{}] after Order event [{}]", collectionId, order.id)
        } else {
            logger.info(
                "Collection [{}] not changed after Order event [{}], event won't be published", collectionId, order.id
            )
        }
    }

    private suspend fun saveAndNotify(
        updated: EnrichmentCollection,
        notificationEnabled: Boolean,
        collection: UnionCollection? = null,
        order: UnionOrder? = null,
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
        enrichmentCollection: EnrichmentCollection,
        unionCollection: UnionCollection? = null,
        order: UnionOrder? = null,
        eventTimeMarks: UnionEventTimeMarks?
    ): CollectionUpdateEventDto {
        val dto = enrichmentCollectionService.enrichCollection(
            enrichmentCollection = enrichmentCollection,
            collection = unionCollection,
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
