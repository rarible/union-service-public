package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.ext

fun AuctionDto.getItemId(): ItemIdDto {
    return this.sell.type.ext.itemId!!
}

fun AuctionDto.getSellerOwnershipId(): OwnershipIdDto {
    val itemId = this.sell.type.ext.itemId!!
    return itemId.toOwnership(this.seller.value)
}

fun AuctionDto.getAuctionOwnershipId(): OwnershipIdDto {
    val itemId = this.sell.type.ext.itemId!!
    return itemId.toOwnership(this.contract.value)
}
