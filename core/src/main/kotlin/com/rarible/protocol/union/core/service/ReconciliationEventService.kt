package com.rarible.protocol.union.core.service

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.ReconciliationMarkEvent
import com.rarible.protocol.union.core.model.itemId
import com.rarible.protocol.union.core.model.ownershipId
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.ext
import org.springframework.stereotype.Component

@Component
class ReconciliationEventService(
    private val eventsProducer: RaribleKafkaProducer<ReconciliationMarkEvent>
) {

    suspend fun onCorruptedItem(itemId: ItemIdDto) {
        eventsProducer.send(
            KafkaEventFactory.reconciliationItemMarkEvent(itemId)
        ).ensureSuccess()
    }

    suspend fun onCorruptedOwnership(ownershipId: OwnershipIdDto) {
        eventsProducer.send(
            KafkaEventFactory.reconciliationOwnershipMarkEvent(ownershipId)
        ).ensureSuccess()
    }

    suspend fun onCorruptedCollection(collectionId: CollectionIdDto) {
        eventsProducer.send(
            KafkaEventFactory.reconciliationCollectionMarkEvent(collectionId)
        ).ensureSuccess()
    }

    suspend fun onFailedOrder(order: OrderDto) {
        // TODO collection not supported here
        val makeAssetExt = order.make.type.ext
        val takeAssetExt = order.take.type.ext

        val makeItemId = makeAssetExt.itemId
        val takeItemId = takeAssetExt.itemId

        if (makeItemId != null) {
            val ownershipId = makeItemId.toOwnership(order.maker.value)
            onCorruptedItem(makeItemId)
            onCorruptedOwnership(ownershipId)
        }

        if (takeItemId != null) {
            onCorruptedItem(takeItemId)
        }
    }

    suspend fun onFailedActivity(activity: ActivityDto) {
        activity.itemId()?.let { onCorruptedItem(it) }
        activity.ownershipId()?.let { onCorruptedOwnership(it) }
    }

}
