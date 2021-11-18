package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.BlockchainDto

sealed class UnionAuctionEvent {

    abstract val auctionId: AuctionIdDto
}

data class UnionAuctionUpdateEvent(
    override val auctionId: AuctionIdDto,
    val auction: AuctionDto
) : UnionAuctionEvent() {

    constructor(auction: AuctionDto) : this(auction.id, auction)

}

data class UnionAuctionDeleteEvent(
    override val auctionId: AuctionIdDto
) : UnionAuctionEvent()
