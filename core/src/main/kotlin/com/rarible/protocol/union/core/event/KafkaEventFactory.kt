package com.rarible.protocol.union.core.event

import com.rarible.core.kafka.KafkaMessage
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionWrappedEvent
import com.rarible.protocol.union.core.model.UnionWrappedItemEvent
import com.rarible.protocol.union.core.model.UnionWrappedOrderEvent
import com.rarible.protocol.union.core.model.UnionWrappedOwnershipEvent
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.dto.ext
import java.util.*

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
            dto.ownershipId.contract,
            dto.ownershipId.tokenId
        )
        return KafkaMessage(
            id = dto.eventId,
            key = itemId.fullId(),
            value = dto,
            headers = OWNERSHIP_EVENT_HEADERS
        )
    }

    fun wrappedItemEvent(event: UnionItemEvent): KafkaMessage<UnionWrappedEvent> {
        return KafkaMessage(
            id = UUID.randomUUID().toString(),
            key = event.itemId.fullId(),
            value = UnionWrappedItemEvent(event),
            headers = ITEM_EVENT_HEADERS
        )
    }

    fun wrappedOwnershipEvent(event: UnionOwnershipEvent): KafkaMessage<UnionWrappedEvent> {
        val ownershipId = event.ownershipId
        val itemId = ItemIdDto(
            ownershipId.blockchain,
            ownershipId.contract,
            ownershipId.tokenId
        )
        return KafkaMessage(
            id = UUID.randomUUID().toString(),
            key = itemId.fullId(),
            value = UnionWrappedOwnershipEvent(event),
            headers = OWNERSHIP_EVENT_HEADERS
        )
    }

    fun wrappedOrderEvent(event: UnionOrderEvent): KafkaMessage<UnionWrappedEvent> {
        val order = event.order

        val makeAssetExt = order.make.type.ext
        val takeAssetExt = order.take.type.ext

        val key = when {
            makeAssetExt.isCollection -> ContractAddress(order.id.blockchain, makeAssetExt.contract).fullId()
            takeAssetExt.isCollection -> ContractAddress(order.id.blockchain, takeAssetExt.contract).fullId()
            makeAssetExt.itemId != null -> makeAssetExt.itemId!!.fullId()
            takeAssetExt.itemId != null -> takeAssetExt.itemId!!.fullId()
            else -> order.id.fullId()
        }

        return KafkaMessage(
            id = UUID.randomUUID().toString(),
            key = key,
            value = UnionWrappedOrderEvent(event),
            headers = ORDER_EVENT_HEADERS
        )
    }

}