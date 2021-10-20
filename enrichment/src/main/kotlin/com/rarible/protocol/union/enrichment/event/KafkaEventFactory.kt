package com.rarible.protocol.union.enrichment.event

import com.rarible.core.kafka.KafkaMessage
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider

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
        val itemId = ItemIdDto(
            dto.ownershipId.blockchain,
            dto.ownershipId.token,
            dto.ownershipId.tokenId
        )
        return KafkaMessage(
            id = dto.eventId,
            key = itemId.fullId(),
            value = dto,
            headers = OWNERSHIP_EVENT_HEADERS
        )
    }


}