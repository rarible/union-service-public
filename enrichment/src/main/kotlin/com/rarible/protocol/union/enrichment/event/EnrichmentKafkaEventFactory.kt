package com.rarible.protocol.union.enrichment.event

import com.rarible.core.kafka.KafkaMessage
import com.rarible.protocol.union.core.model.ReconciliationMarkEvent
import com.rarible.protocol.union.core.model.ReconciliationMarkType
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.model.ItemChangeEvent
import java.util.UUID

object EnrichmentKafkaEventFactory {

    fun reconciliationItemMarkEvent(itemId: ItemIdDto): KafkaMessage<ReconciliationMarkEvent> {
        return KafkaMessage(
            id = UUID.randomUUID().toString(),
            key = itemId.fullId(),
            value = ReconciliationMarkEvent(itemId.fullId(), ReconciliationMarkType.ITEM),
        )
    }

    fun reconciliationOwnershipMarkEvent(ownershipId: OwnershipIdDto): KafkaMessage<ReconciliationMarkEvent> {
        return KafkaMessage(
            id = UUID.randomUUID().toString(),
            key = ownershipId.fullId(),
            value = ReconciliationMarkEvent(ownershipId.fullId(), ReconciliationMarkType.OWNERSHIP),
        )
    }

    fun reconciliationCollectionMarkEvent(collectionId: CollectionIdDto): KafkaMessage<ReconciliationMarkEvent> {
        return KafkaMessage(
            id = UUID.randomUUID().toString(),
            key = collectionId.fullId(),
            value = ReconciliationMarkEvent(collectionId.fullId(), ReconciliationMarkType.COLLECTION)
        )
    }

    fun downloadTaskEvent(task: DownloadTaskEvent): KafkaMessage<DownloadTaskEvent> {
        return KafkaMessage(
            id = UUID.randomUUID().toString(),
            key = task.id,
            value = task
        )
    }

    fun itemChangeEvent(event: ItemChangeEvent): KafkaMessage<ItemChangeEvent> {
        return KafkaMessage(
            id = UUID.randomUUID().toString(),
            key = event.id.toDto().fullId(),
            value = event
        )
    }
}
