package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "ITEM", value = UnionInternalItemEvent::class),
    JsonSubTypes.Type(name = "OWNERSHIP", value = UnionInternalOwnershipEvent::class),
    JsonSubTypes.Type(name = "ORDER", value = UnionInternalOrderEvent::class),
    JsonSubTypes.Type(name = "AUCTION", value = UnionAuctionEvent::class),
    JsonSubTypes.Type(name = "ACTIVITY", value = UnionInternalActivityEvent::class),
    // TODO remove this one later, everything is serialized with "ACTIVITY" now
    JsonSubTypes.Type(name = "ACTIVITY_", value = UnionInternalActivityEvent::class)
)
sealed class UnionInternalBlockchainEvent {
    abstract fun getEntityId(): Any
}

data class UnionInternalItemEvent(
    val event: UnionItemEvent
) : UnionInternalBlockchainEvent() {
    override fun getEntityId() = event.itemId
}

data class UnionInternalCollectionEvent(
    val event: UnionCollectionEvent
) : UnionInternalBlockchainEvent() {
    override fun getEntityId() = event.collectionId
}

data class UnionInternalOwnershipEvent(
    val event: UnionOwnershipEvent
) : UnionInternalBlockchainEvent() {
    override fun getEntityId() = event.ownershipId
}

data class UnionInternalOrderEvent(
    val event: UnionOrderEvent
) : UnionInternalBlockchainEvent() {
    override fun getEntityId() = event.orderId
}

data class UnionInternalAuctionEvent(
    val event: UnionAuctionEvent
) : UnionInternalBlockchainEvent() {
    override fun getEntityId() = event.auction.auctionId
}

data class UnionInternalActivityEvent(
    val event: UnionActivity
) : UnionInternalBlockchainEvent() {
    override fun getEntityId() = event.id
}