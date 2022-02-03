package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.protocol.union.dto.AuctionDto

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "UPDATE", value = UnionAuctionUpdateEvent::class),
    JsonSubTypes.Type(name = "DELETE", value = UnionAuctionDeleteEvent::class)
)
sealed class UnionAuctionEvent {
    abstract val auction: AuctionDto
}

data class UnionAuctionUpdateEvent(
    override val auction: AuctionDto
) : UnionAuctionEvent()

data class UnionAuctionDeleteEvent(
    override val auction: AuctionDto
) : UnionAuctionEvent()
