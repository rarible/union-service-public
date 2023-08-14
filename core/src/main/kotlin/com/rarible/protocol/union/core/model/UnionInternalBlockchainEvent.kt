package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.dto.BlockchainDto

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
    abstract fun getBlockchain(): BlockchainDto
    abstract fun getEventType(): EventType
}

data class UnionInternalItemEvent(
    val event: UnionItemEvent
) : UnionInternalBlockchainEvent() {
    override fun getEntityId() = event.itemId
    override fun getBlockchain(): BlockchainDto = event.itemId.blockchain
    override fun getEventType(): EventType = EventType.ITEM
}

data class UnionInternalCollectionEvent(
    val event: UnionCollectionEvent
) : UnionInternalBlockchainEvent() {
    override fun getEntityId() = event.collectionId
    override fun getBlockchain(): BlockchainDto = event.collectionId.blockchain
    override fun getEventType(): EventType = EventType.COLLECTION
}

data class UnionInternalOwnershipEvent(
    val event: UnionOwnershipEvent
) : UnionInternalBlockchainEvent() {
    override fun getEntityId() = event.ownershipId
    override fun getBlockchain(): BlockchainDto = event.ownershipId.blockchain
    override fun getEventType(): EventType = EventType.OWNERSHIP
}

data class UnionInternalOrderEvent(
    val event: UnionOrderEvent
) : UnionInternalBlockchainEvent() {
    override fun getEntityId() = event.orderId
    override fun getBlockchain(): BlockchainDto = event.orderId.blockchain
    override fun getEventType(): EventType = EventType.ORDER
}

data class UnionInternalAuctionEvent(
    val event: UnionAuctionEvent
) : UnionInternalBlockchainEvent() {
    override fun getEntityId() = event.auction.auctionId
    override fun getBlockchain(): BlockchainDto = event.auction.id.blockchain
    override fun getEventType(): EventType = EventType.AUCTION
}

data class UnionInternalActivityEvent(
    val event: UnionActivity
) : UnionInternalBlockchainEvent() {
    override fun getEntityId() = event.id
    override fun getBlockchain(): BlockchainDto = event.id.blockchain
    override fun getEventType(): EventType = EventType.ACTIVITY
}
