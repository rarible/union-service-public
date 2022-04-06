package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.*

fun ActivityDto.itemId(): ItemIdDto? {
    return when (this) {
        is MintActivityDto -> this.itemId
        is TransferActivityDto -> this.itemId
        is BurnActivityDto -> this.itemId

        is OrderMatchSellDto -> this.nft.type.ext.itemId
        is OrderMatchSwapDto -> null
        is OrderListActivityDto -> this.make.type.ext.itemId
        is OrderBidActivityDto -> this.take.type.ext.itemId
        is OrderCancelListActivityDto -> this.make.ext.itemId
        is OrderCancelBidActivityDto -> this.take.ext.itemId

        is AuctionOpenActivityDto -> this.auction.getItemId()
        is AuctionCancelActivityDto -> this.auction.getItemId()
        is AuctionBidActivityDto -> this.auction.getItemId()
        is AuctionFinishActivityDto -> this.auction.getItemId()
        is AuctionStartActivityDto -> this.auction.getItemId()
        is AuctionEndActivityDto -> this.auction.getItemId()
        is L2DepositActivityDto -> ItemIdDto(this.id.blockchain, this.contractAddres.value, this.tokenId)
        is L2WithdrawalActivityDto -> ItemIdDto(this.id.blockchain, this.contractAddres.value, this.tokenId)
    }
}

fun ActivityDto.ownershipId(): OwnershipIdDto? {
    return when (this) {
        is MintActivityDto -> this.itemId?.toOwnership(this.owner.value)
        is TransferActivityDto -> this.itemId?.toOwnership(this.owner.value)
        else -> null
    }
}

fun ActivityDto.source(): OwnershipSourceDto? {
    return when (this) {
        is MintActivityDto -> OwnershipSourceDto.MINT
        is TransferActivityDto -> this.purchase?.let {
            if (it) OwnershipSourceDto.PURCHASE else OwnershipSourceDto.TRANSFER
        }
        else -> null
    }
}
