package com.rarible.protocol.union.core.converter

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

    fun convert(source: ActivityDto): EsActivity {
        return when (source) {
            is MintActivityDto -> convertMint(source)
            is BurnActivityDto -> convertBurn(source)
            is TransferActivityDto -> convertTransfer(source)
            is OrderMatchSwapDto -> convertOrderMatchSwap(source)
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
            user = singleUser(source.owner),
            collection = singleCollection(itemId),
            item = singleItem(itemId)
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
            user = singleUser(source.owner),
            collection = singleCollection(itemId),
            item = singleItem(itemId)
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
            user = bothUsers(source.from, source.owner),
            collection = singleCollection(itemId),
            item = singleItem(itemId)
        )
    }

    private fun convertOrderMatchSwap(source: OrderMatchSwapDto): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.SELL,
            user = bothUsers(source.left.maker, source.right.maker),
            collection = bothCollections(source.left.asset, source.right.asset),
            item = bothItems(source.left.asset, source.right.asset),
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
            user = bothUsers(source.seller, source.buyer),
            collection = bothCollections(source.nft, source.payment),
            item = bothItems(source.nft, source.payment),
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
            user = singleUser(source.maker),
            collection = bothCollections(source.make, source.take),
            item = bothItems(source.make, source.take),
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
            user = singleUser(source.maker),
            collection = bothCollections(source.make, source.take),
            item = bothItems(source.make, source.take),
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
            user = singleUser(source.maker),
            collection = bothCollections(source.make, source.take),
            item = bothItems(source.make, source.take),
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
            user = singleUser(source.maker),
            collection = bothCollections(source.make, source.take),
            item = bothItems(source.make, source.take),
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
            user = singleUser(source.auction.seller),
            collection = bothCollections(source.auction.sell.type, source.auction.buy),
            item = bothItems(source.auction.sell.type, source.auction.buy),
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
            user = bothUsers(source.auction.seller, source.bid.buyer),
            collection = bothCollections(source.auction.sell.type, source.auction.buy),
            item = bothItems(source.auction.sell.type, source.auction.buy),
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
            user = singleUser(source.auction.seller),
            collection = bothCollections(source.auction.sell.type, source.auction.buy),
            item = bothItems(source.auction.sell.type, source.auction.buy),
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
            user = singleUser(source.auction.seller),
            collection = bothCollections(source.auction.sell.type, source.auction.buy),
            item = bothItems(source.auction.sell.type, source.auction.buy),
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
            user = singleUser(source.auction.seller),
            collection = bothCollections(source.auction.sell.type, source.auction.buy),
            item = bothItems(source.auction.sell.type, source.auction.buy),
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
            user = singleUser(source.auction.seller),
            collection = bothCollections(source.auction.sell.type, source.auction.buy),
            item = bothItems(source.auction.sell.type, source.auction.buy),
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
            user = singleUser(source.user),
            collection = singleCollection(source.itemId),
            item = singleItem(source.itemId)
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
            user = singleUser(source.user),
            collection = singleCollection(source.itemId),
            item = singleItem(source.itemId),
        )
    }

    private fun singleUser(user: UnionAddress): EsActivity.User {
        return EsActivity.User(
            maker = user.value,
            taker = null,
        )
    }

    private fun singleCollection(itemId: ItemIdDto): EsActivity.Collection {
        return EsActivity.Collection(
            make = IdParser.extractContract(itemId),
            take = null,
        )
    }

    private fun singleItem(itemId: ItemIdDto): EsActivity.Item {
        return EsActivity.Item(
            make = itemId.value,
            take = null,
        )
    }

    private fun bothUsers(maker: UnionAddress, taker: UnionAddress): EsActivity.User {
        return EsActivity.User(
            maker = maker.value,
            taker = taker.value,
        )
    }

    private fun bothCollections(left: AssetDto, right: AssetDto): EsActivity.Collection {
        return bothCollections(left.type, right.type)
    }

    private fun bothCollections(left: AssetTypeDto, right: AssetTypeDto): EsActivity.Collection {
        return EsActivity.Collection(
            make = left.ext.getCollections(),
            take = right.ext.getCollections(),
        )
    }

    private fun bothItems(left: AssetDto, right: AssetDto): EsActivity.Item {
        return bothItems(left.type, right.type)
    }

    private fun bothItems(left: AssetTypeDto, right: AssetTypeDto): EsActivity.Item {
        return EsActivity.Item(
            make = left.ext.itemId?.toString().orEmpty(),
            take = right.ext.itemId?.toString().orEmpty(),
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
