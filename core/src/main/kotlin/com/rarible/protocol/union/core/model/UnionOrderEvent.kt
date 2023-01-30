package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "UPDATE", value = UnionOrderUpdateEvent::class),
    JsonSubTypes.Type(name = "UPDATE_POOL_NFT", value = UnionPoolNftUpdateEvent::class),
    JsonSubTypes.Type(name = "UPDATE_POOL_ORDER", value = UnionPoolOrderUpdateEvent::class)
)
sealed class UnionOrderEvent {

    abstract val orderId: OrderIdDto
    abstract val eventTimeMarks: UnionEventTimeMarks?
    abstract fun addTimeMark(name: String, date: Instant? = null): UnionOrderEvent
}

data class UnionOrderUpdateEvent(
    override val orderId: OrderIdDto,
    val order: OrderDto,
    override val eventTimeMarks: UnionEventTimeMarks?
) : UnionOrderEvent() {

    constructor(order: OrderDto, eventTimeMarks: UnionEventTimeMarks?) : this(order.id, order, eventTimeMarks)

    override fun addTimeMark(name: String, date: Instant?): UnionOrderUpdateEvent {
        return this.copy(eventTimeMarks = this.eventTimeMarks?.add(name, date))
    }
}

// Event received from blockchains
data class UnionPoolNftUpdateEvent(
    override val orderId: OrderIdDto,
    val inNft: Set<ItemIdDto>,
    val outNft: Set<ItemIdDto>,
    override val eventTimeMarks: UnionEventTimeMarks?
) : UnionOrderEvent() {

    override fun addTimeMark(name: String, date: Instant?): UnionPoolNftUpdateEvent {
        return this.copy(eventTimeMarks = this.eventTimeMarks?.add(name, date))
    }
}

// Synthetic event based on inNft/outNft data from UnionPoolNftUpdateEvent
data class UnionPoolOrderUpdateEvent(
    override val orderId: OrderIdDto,
    val order: OrderDto,
    val itemId: ItemIdDto,
    val action: PoolItemAction,
    override val eventTimeMarks: UnionEventTimeMarks?
) : UnionOrderEvent() {

    constructor(
        order: OrderDto,
        itemId: ItemIdDto,
        action: PoolItemAction,
        eventTimeMarks: UnionEventTimeMarks?
    ) : this(
        order.id,
        order,
        itemId,
        action,
        eventTimeMarks
    )

    // There is no need to include synthetic orders into mark chain (in/out will be duplicated so)
    override fun addTimeMark(name: String, date: Instant?) = this
}

enum class PoolItemAction {
    INCLUDED,
    EXCLUDED,
    UPDATED
}