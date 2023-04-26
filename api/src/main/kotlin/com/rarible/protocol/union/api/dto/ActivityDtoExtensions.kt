package com.rarible.protocol.union.api.dto

import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.AuctionBidActivityDto
import com.rarible.protocol.union.dto.AuctionCancelActivityDto
import com.rarible.protocol.union.dto.AuctionEndActivityDto
import com.rarible.protocol.union.dto.AuctionFinishActivityDto
import com.rarible.protocol.union.dto.AuctionOpenActivityDto
import com.rarible.protocol.union.dto.AuctionStartActivityDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.L2DepositActivityDto
import com.rarible.protocol.union.dto.L2WithdrawalActivityDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelListActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.OrderMatchSwapDto
import com.rarible.protocol.union.dto.TransferActivityDto

fun ActivityDto?.applyCursor(cursor: String): ActivityDto? {
    if (this == null) return null
    return when (this) {
        is MintActivityDto -> copy(cursor = cursor)
        is BurnActivityDto -> copy(cursor = cursor)
        is TransferActivityDto -> copy(cursor = cursor)
        is OrderMatchSwapDto -> copy(cursor = cursor)
        is OrderMatchSellDto -> copy(cursor = cursor)
        is OrderBidActivityDto -> copy(cursor = cursor)
        is OrderListActivityDto -> copy(cursor = cursor)
        is OrderCancelBidActivityDto -> copy(cursor = cursor)
        is OrderCancelListActivityDto -> copy(cursor = cursor)
        is AuctionOpenActivityDto -> copy(cursor = cursor)
        is AuctionBidActivityDto -> copy(cursor = cursor)
        is AuctionFinishActivityDto -> copy(cursor = cursor)
        is AuctionCancelActivityDto -> copy(cursor = cursor)
        is AuctionStartActivityDto -> copy(cursor = cursor)
        is AuctionEndActivityDto -> copy(cursor = cursor)
        is L2DepositActivityDto -> copy(cursor = cursor)
        is L2WithdrawalActivityDto -> copy(cursor = cursor)
    }
}