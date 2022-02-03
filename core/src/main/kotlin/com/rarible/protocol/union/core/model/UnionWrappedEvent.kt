package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "ITEM", value = UnionWrappedItemEvent::class),
    JsonSubTypes.Type(name = "OWNERSHIP", value = UnionWrappedOwnershipEvent::class),
    JsonSubTypes.Type(name = "ORDER", value = UnionWrappedOrderEvent::class)
)
sealed class UnionWrappedEvent

data class UnionWrappedItemEvent(
    val event: UnionItemEvent
) : UnionWrappedEvent()

data class UnionWrappedOwnershipEvent(
    val event: UnionOwnershipEvent
) : UnionWrappedEvent()

data class UnionWrappedOrderEvent(
    val event: UnionOrderEvent
) : UnionWrappedEvent()

data class UnionWrappedAuctionEvent(
    val event: UnionAuctionEvent
) : UnionWrappedEvent()