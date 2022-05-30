package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.AssetTypeExtension
import com.rarible.protocol.union.dto.AuctionBidActivityDto
import com.rarible.protocol.union.dto.AuctionCancelActivityDto
import com.rarible.protocol.union.dto.AuctionEndActivityDto
import com.rarible.protocol.union.dto.AuctionFinishActivityDto
import com.rarible.protocol.union.dto.AuctionOpenActivityDto
import com.rarible.protocol.union.dto.AuctionStartActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
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
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.dto.parser.IdParser

object EsActivityConverter {

    fun convert(source: ActivityDto, collection: String?): EsActivity? {
        return when (source) {
            is MintActivityDto -> convertMint(source, collection)
            is BurnActivityDto -> convertBurn(source, collection)
            is TransferActivityDto -> convertTransfer(source, collection)
            is OrderMatchSwapDto -> null
            is OrderMatchSellDto -> convertOrderMatchSell(source, collection)
            is OrderBidActivityDto -> convertOrderBid(source, collection)
            is OrderListActivityDto -> convertOrderList(source, collection)
            is OrderCancelBidActivityDto -> convertOrderCancelBid(source, collection)
            is OrderCancelListActivityDto -> convertOrderCancelList(source, collection)
            is AuctionOpenActivityDto -> convertAuctionOpen(source, collection)
            is AuctionBidActivityDto -> convertAuctionBid(source, collection)
            is AuctionFinishActivityDto -> convertAuctionFinish(source, collection)
            is AuctionCancelActivityDto -> convertAuctionCancel(source, collection)
            is AuctionStartActivityDto -> convertAuctionStart(source, collection)
            is AuctionEndActivityDto -> convertAuctionEnd(source, collection)
            is L2WithdrawalActivityDto -> convertL2Withdrawal(source, collection)
            is L2DepositActivityDto -> convertL2Deposit(source, collection)
        }
    }

    fun extractItemId(source: ActivityDto): ItemIdDto? {
        return when (source) {
            is MintActivityDto -> source.itemId
            is BurnActivityDto -> source.itemId
            is TransferActivityDto -> source.itemId
            is OrderMatchSwapDto -> null
            is OrderMatchSellDto -> source.nft.type.ext.itemId
            is OrderBidActivityDto -> source.take.type.ext.itemId
            is OrderListActivityDto -> source.make.type.ext.itemId
            is OrderCancelBidActivityDto -> source.take.ext.itemId
            is OrderCancelListActivityDto -> source.make.ext.itemId
            is AuctionOpenActivityDto -> source.auction.sell.type.ext.itemId
            is AuctionBidActivityDto -> source.auction.sell.type.ext.itemId
            is AuctionFinishActivityDto -> source.auction.sell.type.ext.itemId
            is AuctionCancelActivityDto -> source.auction.sell.type.ext.itemId
            is AuctionStartActivityDto -> source.auction.sell.type.ext.itemId
            is AuctionEndActivityDto -> source.auction.sell.type.ext.itemId
            is L2WithdrawalActivityDto -> source.itemId
            is L2DepositActivityDto -> source.itemId
        }
    }

    private fun calcCollection(source: ActivityDto, itemIdOrNull: ItemIdDto?, override: String?): String? {
        if (override != null) return override
        // As per ALPHA-454 it is expected to have events with null itemId, e.g. for floor bids
        val itemId = itemIdOrNull ?: ItemIdDto(source.id.blockchain, "")
        return when (source) {
            is MintActivityDto -> IdParser.extractContract(itemId)
            is BurnActivityDto -> IdParser.extractContract(itemId)
            is TransferActivityDto -> IdParser.extractContract(itemId)
            is OrderMatchSwapDto -> null
            is OrderMatchSellDto -> source.nft.type.ext.getCollections()
            is OrderBidActivityDto -> source.take.type.ext.getCollections()
            is OrderListActivityDto -> source.make.type.ext.getCollections()
            is OrderCancelBidActivityDto -> source.take.ext.getCollections()
            is OrderCancelListActivityDto -> source.make.ext.getCollections()
            is AuctionOpenActivityDto -> source.auction.sell.type.ext.getCollections()
            is AuctionBidActivityDto -> source.auction.sell.type.ext.getCollections()
            is AuctionFinishActivityDto -> source.auction.sell.type.ext.getCollections()
            is AuctionCancelActivityDto -> source.auction.sell.type.ext.getCollections()
            is AuctionStartActivityDto -> source.auction.sell.type.ext.getCollections()
            is AuctionEndActivityDto -> source.auction.sell.type.ext.getCollections()
            is L2WithdrawalActivityDto -> IdParser.extractContract(itemId)
            is L2DepositActivityDto -> IdParser.extractContract(itemId)
        }
    }


    private fun convertMint(source: MintActivityDto, collection: String?): EsActivity {
        val itemId = extractItemId(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.MINT,
            userTo = source.owner.value,
            userFrom = null,
            collection = calcCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertBurn(source: BurnActivityDto, collection: String?): EsActivity {
        val itemId = extractItemId(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.BURN,
            userFrom = source.owner.value,
            userTo = null,
            collection = calcCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertTransfer(source: TransferActivityDto, collection: String?): EsActivity {
        val itemId = extractItemId(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.TRANSFER,
            userFrom = source.from.value,
            userTo = source.owner.value,
            collection = calcCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertOrderMatchSell(source: OrderMatchSellDto, collection: String?): EsActivity {
        val itemId = extractItemId(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.SELL,
            userFrom = source.seller.value,
            userTo = source.buyer.value,
            collection = calcCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertOrderBid(source: OrderBidActivityDto, collection: String?): EsActivity {
        val itemId = extractItemId(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.BID,
            userFrom = source.maker.value,
            userTo = null,
            collection = calcCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertOrderList(source: OrderListActivityDto, collection: String?): EsActivity {
        val itemId = extractItemId(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.LIST,
            userFrom = source.maker.value,
            userTo = null,
            collection = calcCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertOrderCancelBid(source: OrderCancelBidActivityDto, collection: String?): EsActivity {
        val itemId = extractItemId(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.CANCEL_BID,
            userFrom = source.maker.value,
            userTo = null,
            collection = calcCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertOrderCancelList(source: OrderCancelListActivityDto, collection: String?): EsActivity {
        val itemId = extractItemId(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.CANCEL_LIST,
            userFrom = source.maker.value,
            userTo = null,
            collection = calcCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertAuctionOpen(source: AuctionOpenActivityDto, collection: String?): EsActivity {
        val itemId = extractItemId(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_CREATED,
            userFrom = source.auction.seller.value,
            userTo = null,
            collection = calcCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertAuctionBid(source: AuctionBidActivityDto, collection: String?): EsActivity {
        val itemId = extractItemId(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_BID,
            userFrom = source.auction.seller.value,
            userTo = source.bid.buyer.value,
            collection = calcCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertAuctionFinish(source: AuctionFinishActivityDto, collection: String?): EsActivity {
        val itemId = extractItemId(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_FINISHED,
            userFrom = source.auction.seller.value,
            userTo = null,
            collection = calcCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertAuctionCancel(source: AuctionCancelActivityDto, collection: String?): EsActivity {
        val itemId = extractItemId(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_CANCEL,
            userFrom = source.auction.seller.value,
            userTo = null,
            collection = calcCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertAuctionStart(source: AuctionStartActivityDto, collection: String?): EsActivity {
        val itemId = extractItemId(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_STARTED,
            userFrom = source.auction.seller.value,
            userTo = null,
            collection = calcCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertAuctionEnd(source: AuctionEndActivityDto, collection: String?): EsActivity {
        val itemId = extractItemId(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_ENDED,
            userFrom = source.auction.seller.value,
            userTo = null,
            collection = calcCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertL2Withdrawal(source: L2WithdrawalActivityDto, collection: String?): EsActivity {
        val itemId = extractItemId(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.TRANSFER, // TODO
            userFrom = null,
            userTo = source.user.value,
            collection = calcCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertL2Deposit(source: L2DepositActivityDto, collection: String?): EsActivity {
        val itemId = extractItemId(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date,
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.TRANSFER, // TODO
            userFrom = source.user.value,
            userTo = null,
            collection = calcCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun AssetTypeExtension.getCollections(): String? {
        return when {
            isCurrency -> currencyAddress()
            collectionId != null -> collectionId!!.value
            itemId != null -> IdParser.extractContract(itemId!!)
            else -> null
        }
    }
}
