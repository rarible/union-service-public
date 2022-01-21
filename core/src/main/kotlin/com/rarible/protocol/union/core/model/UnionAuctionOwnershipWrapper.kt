package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import java.time.Instant

// For sorting purposes
class UnionAuctionOwnershipWrapper(
    val ownership: UnionOwnership?,
    val auction: AuctionDto?
) {

    val ownershipId: OwnershipIdDto = ownership?.id ?: auction!!.getSellerOwnershipId()
    val date: Instant = ownership?.createdAt ?: auction!!.createdAt

}