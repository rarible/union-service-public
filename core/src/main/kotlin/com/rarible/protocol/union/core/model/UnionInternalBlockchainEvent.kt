package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.ActivityDto

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "ITEM", value = UnionInternalItemEvent::class),
    JsonSubTypes.Type(name = "OWNERSHIP", value = UnionInternalOwnershipEvent::class),
    JsonSubTypes.Type(name = "ORDER", value = UnionInternalOrderEvent::class),
    JsonSubTypes.Type(name = "AUCTION", value = UnionAuctionEvent::class),
    JsonSubTypes.Type(name = "ACTIVITY", value = UnionInternalActivityLegacyEvent::class),
    // TODO rename to ACTIVITY after few releases
    JsonSubTypes.Type(name = "ACTIVITY_", value = UnionInternalActivityEvent::class)
)
sealed class UnionInternalBlockchainEvent

data class UnionInternalItemEvent(
    val event: UnionItemEvent
) : UnionInternalBlockchainEvent()

data class UnionInternalCollectionEvent(
    val event: UnionCollectionEvent
) : UnionInternalBlockchainEvent()

data class UnionInternalOwnershipEvent(
    val event: UnionOwnershipEvent
) : UnionInternalBlockchainEvent()

data class UnionInternalOrderEvent(
    val event: UnionOrderEvent
) : UnionInternalBlockchainEvent()

data class UnionInternalAuctionEvent(
    val event: UnionAuctionEvent
) : UnionInternalBlockchainEvent()

data class UnionInternalActivityLegacyEvent(
    val event: ActivityDto
) : UnionInternalBlockchainEvent()

data class UnionInternalActivityEvent(
    val event: UnionActivity
) : UnionInternalBlockchainEvent()