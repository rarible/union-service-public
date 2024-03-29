package com.rarible.protocol.union.enrichment.service

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.model.ReconciliationMarkEvent
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.enrichment.event.EnrichmentKafkaEventFactory
import org.springframework.stereotype.Component

@Component
class ReconciliationEventService(
    private val eventsProducer: RaribleKafkaProducer<ReconciliationMarkEvent>
) {

    suspend fun onCorruptedItem(itemId: ItemIdDto) {
        eventsProducer.send(
            EnrichmentKafkaEventFactory.reconciliationItemMarkEvent(itemId)
        ).ensureSuccess()
    }

    suspend fun onCorruptedOwnership(ownershipId: OwnershipIdDto) {
        eventsProducer.send(
            EnrichmentKafkaEventFactory.reconciliationOwnershipMarkEvent(ownershipId)
        ).ensureSuccess()
    }

    suspend fun onCorruptedCollection(collectionId: CollectionIdDto) {
        eventsProducer.send(
            EnrichmentKafkaEventFactory.reconciliationCollectionMarkEvent(collectionId)
        ).ensureSuccess()
    }

    suspend fun onFailedOrder(order: UnionOrder) {
        // TODO collection not supported here
        val makeAssetExt = order.make.type
        val takeAssetExt = order.take.type

        val makeItemId = makeAssetExt.itemId()
        val takeItemId = takeAssetExt.itemId()

        if (makeItemId != null) {
            val ownershipId = makeItemId.toOwnership(order.maker.value)
            onCorruptedItem(makeItemId)
            onCorruptedOwnership(ownershipId)
        }

        if (takeItemId != null) {
            onCorruptedItem(takeItemId)
        }
    }

    suspend fun onFailedActivity(activity: UnionActivity) {
        activity.itemId()?.let { onCorruptedItem(it) }
        activity.ownershipId()?.let { onCorruptedOwnership(it) }
    }
}
