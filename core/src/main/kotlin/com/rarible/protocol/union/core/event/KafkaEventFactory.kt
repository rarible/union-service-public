package com.rarible.protocol.union.core.event

import com.rarible.core.kafka.KafkaMessage
import com.rarible.protocol.union.core.model.ReconciliationMarkEvent
import com.rarible.protocol.union.core.model.ReconciliationMarkType
import com.rarible.protocol.union.core.model.UnionAuctionEvent
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionInternalActivityEvent
import com.rarible.protocol.union.core.model.UnionInternalAuctionEvent
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.core.model.UnionInternalCollectionEvent
import com.rarible.protocol.union.core.model.UnionInternalItemEvent
import com.rarible.protocol.union.core.model.UnionInternalOrderEvent
import com.rarible.protocol.union.core.model.UnionInternalOwnershipEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.getItemId
import com.rarible.protocol.union.core.model.itemId
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.dto.ext
import java.util.UUID

object KafkaEventFactory {

    private val ITEM_EVENT_HEADERS = mapOf(
        "protocol.union.item.event.version" to UnionEventTopicProvider.VERSION
    )

    private val OWNERSHIP_EVENT_HEADERS = mapOf(
        "protocol.union.ownership.event.version" to UnionEventTopicProvider.VERSION
    )

    private val ORDER_EVENT_HEADERS = mapOf(
        "protocol.union.order.event.version" to UnionEventTopicProvider.VERSION
    )

    private val ACTIVITY_EVENT_HEADERS = mapOf(
        "protocol.union.activity.event.version" to UnionEventTopicProvider.VERSION
    )

    private val COLLECTION_EVENT_HEADERS = mapOf(
        "protocol.union.collection.event.version" to UnionEventTopicProvider.VERSION
    )

    private val AUCTION_EVENT_HEADERS = mapOf(
        "protocol.union.auction.event.version" to UnionEventTopicProvider.VERSION
    )

    fun activityEvent(dto: ActivityDto): KafkaMessage<ActivityDto> {
        return KafkaMessage(
            key = dto.id.fullId(), // TODO we need to use right key here
            value = dto,
            headers = ACTIVITY_EVENT_HEADERS
        )
    }

    fun orderEvent(dto: OrderEventDto): KafkaMessage<OrderEventDto> {
        return KafkaMessage(
            key = dto.orderId.fullId(),
            value = dto,
            headers = ORDER_EVENT_HEADERS
        )
    }

    fun itemEvent(dto: ItemEventDto): KafkaMessage<ItemEventDto> {
        return KafkaMessage(
            id = dto.eventId,
            key = dto.itemId.fullId(),
            value = dto,
            headers = ITEM_EVENT_HEADERS
        )
    }

    fun collectionEvent(dto: CollectionEventDto): KafkaMessage<CollectionEventDto> {
        return KafkaMessage(
            id = dto.eventId,
            key = dto.collectionId.fullId(),
            value = dto,
            headers = COLLECTION_EVENT_HEADERS
        )
    }

    fun ownershipEvent(dto: OwnershipEventDto): KafkaMessage<OwnershipEventDto> {
        return KafkaMessage(
            id = dto.eventId,
            key = dto.ownershipId.getItemId().fullId(),
            value = dto,
            headers = OWNERSHIP_EVENT_HEADERS
        )
    }

    fun internalItemEvent(event: UnionItemEvent): KafkaMessage<UnionInternalBlockchainEvent> {
        return KafkaMessage(
            id = UUID.randomUUID().toString(),
            key = event.itemId.fullId(),
            value = UnionInternalItemEvent(event),
            headers = ITEM_EVENT_HEADERS
        )
    }

    fun internalCollectionEvent(event: UnionCollectionEvent): KafkaMessage<UnionInternalBlockchainEvent> {
        return KafkaMessage(
            id = UUID.randomUUID().toString(),
            key = event.collectionId.fullId(),
            value = UnionInternalCollectionEvent(event),
            headers = COLLECTION_EVENT_HEADERS
        )
    }

    fun internalOwnershipEvent(event: UnionOwnershipEvent): KafkaMessage<UnionInternalBlockchainEvent> {
        return KafkaMessage(
            id = UUID.randomUUID().toString(),
            key = event.ownershipId.getItemId().fullId(),
            value = UnionInternalOwnershipEvent(event),
            headers = OWNERSHIP_EVENT_HEADERS
        )
    }

    fun internalOrderEvent(event: UnionOrderEvent): KafkaMessage<UnionInternalBlockchainEvent> {
        val order = event.order

        val makeAssetExt = order.make.type.ext
        val takeAssetExt = order.take.type.ext

        val key = when {
            makeAssetExt.isCollection -> makeAssetExt.collectionId!!.fullId()
            takeAssetExt.isCollection -> takeAssetExt.collectionId!!.fullId()
            makeAssetExt.itemId != null -> makeAssetExt.itemId!!.fullId()
            takeAssetExt.itemId != null -> takeAssetExt.itemId!!.fullId()
            else -> order.id.fullId()
        }

        return KafkaMessage(
            id = UUID.randomUUID().toString(),
            key = key,
            value = UnionInternalOrderEvent(event),
            headers = ORDER_EVENT_HEADERS
        )
    }

    fun internalAuctionEvent(event: UnionAuctionEvent): KafkaMessage<UnionInternalBlockchainEvent> {
        return KafkaMessage(
            id = UUID.randomUUID().toString(),
            key = event.auction.getItemId().fullId(),
            value = UnionInternalAuctionEvent(event),
            headers = AUCTION_EVENT_HEADERS
        )
    }

    fun internalActivityEvent(dto: ActivityDto): KafkaMessage<UnionInternalBlockchainEvent> {
        val itemId = dto.itemId()

        val key = itemId?.fullId() ?: dto.id.fullId()

        return KafkaMessage(
            key = key,
            value = UnionInternalActivityEvent(dto),
            headers = ACTIVITY_EVENT_HEADERS
        )
    }

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

}
