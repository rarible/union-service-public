package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.ActivityDto

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "ITEM", value = UnionWrappedItemEvent::class),
    JsonSubTypes.Type(name = "OWNERSHIP", value = UnionWrappedOwnershipEvent::class),
    JsonSubTypes.Type(name = "ORDER", value = UnionWrappedOrderEvent::class),
    JsonSubTypes.Type(name = "AUCTION", value = UnionAuctionEvent::class),
    JsonSubTypes.Type(name = "ACTIVITY", value = UnionWrappedActivityEvent::class)
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

data class UnionWrappedActivityEvent(
    val event: ActivityDto
) : UnionWrappedEvent()