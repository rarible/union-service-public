package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.dto.group

fun AuctionDto.getItemId(): ItemIdDto {
    return this.sell.type.ext.itemId!!
}

fun AuctionDto.getSellerOwnershipId(): OwnershipIdDto {
    val itemId = this.sell.type.ext.itemId!!
    return OwnershipIdDto(itemId.blockchain, itemId.contract, itemId.tokenId, this.seller)
}

fun AuctionDto.getAuctionOwnershipId(): OwnershipIdDto {
    val itemId = this.sell.type.ext.itemId!!
    return OwnershipIdDto(
        itemId.blockchain,
        itemId.contract,
        itemId.tokenId,
        UnionAddress(this.contract.blockchain.group(), this.contract.value)
    )
}