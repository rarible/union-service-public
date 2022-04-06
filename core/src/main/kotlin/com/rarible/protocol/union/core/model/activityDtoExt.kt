package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.AuctionBidActivityDto
import com.rarible.protocol.union.dto.AuctionCancelActivityDto
import com.rarible.protocol.union.dto.AuctionEndActivityDto
import com.rarible.protocol.union.dto.AuctionFinishActivityDto
import com.rarible.protocol.union.dto.AuctionOpenActivityDto
import com.rarible.protocol.union.dto.AuctionStartActivityDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.L2DepositActivityDto
import com.rarible.protocol.union.dto.L2WithdrawalActivityDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelListActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.OrderMatchSwapDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipSourceDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.ext

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

        is L2DepositActivityDto -> null // TODO
        is L2WithdrawalActivityDto -> null // TODO
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