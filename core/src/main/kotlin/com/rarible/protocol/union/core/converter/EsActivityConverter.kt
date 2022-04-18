package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.converter.EsActivityConverter.getCollections
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.AssetTypeExtension
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
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.core.model.EsActivity

object EsActivityConverter {

    fun convert(source: ActivityDto): EsActivity? {
        return when (source) {
            is MintActivityDto -> convertMint(source)
            is BurnActivityDto -> convertBurn(source)
            is TransferActivityDto -> convertTransfer(source)
            is OrderMatchSwapDto -> null
            is OrderMatchSellDto -> convertOrderMatchSell(source)
            is OrderBidActivityDto -> convertOrderBid(source)
            is OrderListActivityDto -> convertOrderList(source)
            is OrderCancelBidActivityDto -> convertOrderCancelBid(source)
            is OrderCancelListActivityDto -> convertOrderCancelList(source)
            is AuctionOpenActivityDto -> convertAuctionOpen(source)
            is AuctionBidActivityDto -> convertAuctionBid(source)
            is AuctionFinishActivityDto -> convertAuctionFinish(source)
            is AuctionCancelActivityDto -> convertAuctionCancel(source)
            is AuctionStartActivityDto -> convertAuctionStart(source)
            is AuctionEndActivityDto -> convertAuctionEnd(source)
            is L2WithdrawalActivityDto -> convertL2Withdrawal(source)
            is L2DepositActivityDto -> convertL2Deposit(source)
        }
    }

    private fun convertMint(source: MintActivityDto): EsActivity {
        val itemId = safeItemId(source.itemId)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.MINT,
            userTo = source.owner.value,
            userFrom = null,
            collection = IdParser.extractContract(itemId),
            item = itemId.value,
        )
    }

    private fun convertBurn(source: BurnActivityDto): EsActivity {
        val itemId = safeItemId(source.itemId)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.BURN,
            userFrom = source.owner.value,
            userTo = null,
            collection = IdParser.extractContract(itemId),
            item = itemId.value,
        )
    }

    private fun convertTransfer(source: TransferActivityDto): EsActivity {
        val itemId = safeItemId(source.itemId)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.TRANSFER,
            userFrom = source.from.value,
            userTo = source.owner.value,
            collection = IdParser.extractContract(itemId),
            item = itemId.value,
        )
    }

    private fun convertOrderMatchSell(source: OrderMatchSellDto): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.SELL,
            userFrom = source.seller.value,
            userTo = source.buyer.value,
            collection = source.nft.type.ext.getCollections(),
            item = source.nft.type.ext.itemId?.value.orEmpty(),
        )
    }

    private fun convertOrderBid(source: OrderBidActivityDto): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.BID,
            userFrom = source.maker.value,
            userTo = null,
            collection = source.make.type.ext.getCollections(),
            item = source.make.type.ext.itemId?.value.orEmpty(),
        )
    }

    private fun convertOrderList(source: OrderListActivityDto): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.LIST,
            userFrom = source.maker.value,
            userTo = null,
            collection = source.make.type.ext.getCollections(),
            item = source.make.type.ext.itemId?.value.orEmpty(),
        )
    }

    private fun convertOrderCancelBid(source: OrderCancelBidActivityDto): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.CANCEL_BID,
            userFrom = source.maker.value,
            userTo = null,
            collection = source.make.ext.getCollections(),
            item = source.make.ext.itemId?.value.orEmpty(),
        )
    }

    private fun convertOrderCancelList(source: OrderCancelListActivityDto): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.CANCEL_LIST,
            userFrom = source.maker.value,
            userTo = null,
            collection = source.make.ext.getCollections(),
            item = source.make.ext.itemId?.value.orEmpty(),
        )
    }

    private fun convertAuctionOpen(source: AuctionOpenActivityDto): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_CREATED,
            userFrom = source.auction.seller.value,
            userTo = null,
            collection = source.auction.sell.type.ext.getCollections(),
            item = source.auction.sell.type.ext.itemId?.value.orEmpty(),
        )
    }

    private fun convertAuctionBid(source: AuctionBidActivityDto): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_BID,
            userFrom = source.auction.seller.value,
            userTo = source.bid.buyer.value,
            collection = source.auction.sell.type.ext.getCollections(),
            item = source.auction.sell.type.ext.itemId?.value.orEmpty(),
        )
    }

    private fun convertAuctionFinish(source: AuctionFinishActivityDto): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_FINISHED,
            userFrom = source.auction.seller.value,
            userTo = null,
            collection = source.auction.sell.type.ext.getCollections(),
            item = source.auction.sell.type.ext.itemId?.value.orEmpty(),
        )
    }

    private fun convertAuctionCancel(source: AuctionCancelActivityDto): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_CANCEL,
            userFrom = source.auction.seller.value,
            userTo = null,
            collection = source.auction.sell.type.ext.getCollections(),
            item = source.auction.sell.type.ext.itemId?.value.orEmpty(),
        )
    }

    private fun convertAuctionStart(source: AuctionStartActivityDto): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_STARTED,
            userFrom = source.auction.seller.value,
            userTo = null,
            collection = source.auction.sell.type.ext.getCollections(),
            item = source.auction.sell.type.ext.itemId?.value.orEmpty(),
        )
    }

    private fun convertAuctionEnd(source: AuctionEndActivityDto): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_ENDED,
            userFrom = source.auction.seller.value,
            userTo = null,
            collection = source.auction.sell.type.ext.getCollections(),
            item = source.auction.sell.type.ext.itemId?.value.orEmpty(),
        )
    }

    private fun convertL2Withdrawal(source: L2WithdrawalActivityDto): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.TRANSFER, // TODO
            userFrom = null,
            userTo = source.user.value,
            collection = IdParser.extractContract(source.itemId),
            item = source.itemId.value,
        )
    }

    private fun convertL2Deposit(source: L2DepositActivityDto): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.TRANSFER, // TODO
            userFrom = source.user.value,
            userTo = null,
            collection = IdParser.extractContract(source.itemId),
            item = source.itemId.value,
        )
    }

    private fun safeItemId(itemId: ItemIdDto?): ItemIdDto {
        if (itemId != null) return itemId
        throw IllegalArgumentException("itemId fields is null")
    }

    private fun AssetTypeExtension.getCollections(): String? {
        return when {
            isCurrency -> currencyAddress()
            itemId != null -> IdParser.extractContract(itemId!!)
            collectionId != null -> collectionId!!.value
            else -> null
        }
    }
}
