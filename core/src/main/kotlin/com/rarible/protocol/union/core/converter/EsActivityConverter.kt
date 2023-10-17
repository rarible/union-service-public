package com.rarible.protocol.union.core.converter

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.core.converter.helper.SellActivityEnricher
import com.rarible.protocol.union.core.converter.helper.getCurrencyAddressOrNull
import com.rarible.protocol.union.core.model.elastic.EsActivity
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.truncatedToSeconds
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityTypeDto
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
import org.springframework.stereotype.Component

@Component
class EsActivityConverter(
    private val itemRouter: BlockchainRouter<ItemService>,
    private val sellActivityEnricher: SellActivityEnricher,
) {

    private val blockchainsToQueryItems: Set<BlockchainDto> = setOf(BlockchainDto.SOLANA)

    suspend fun batchConvert(source: List<ActivityDto>): List<EsActivity> {
        val items = source.groupBy { it.id.blockchain }
            .filter { blockchainsToQueryItems.contains(it.key) }
            .mapAsync { (blockchain, activities) ->
                val itemIds = activities.mapNotNull { extractItemId(it)?.value }
                itemRouter.getService(blockchain).getItemsByIds(itemIds)
            }
            .flatten()
            .associateBy { it.id }

        return source.mapNotNull {
            val collection = items[extractItemId(it)]?.collection?.value
            convert(it, collection)
        }
    }

    suspend fun convert(source: ActivityDto, collection: String?): EsActivity? {
        val itemId = extractItemId(source)
        return when (source) {
            is MintActivityDto -> convertMint(source, itemId, collection)
            is BurnActivityDto -> convertBurn(source, itemId, collection)
            is TransferActivityDto -> convertTransfer(source, itemId, collection)
            is OrderMatchSellDto -> convertOrderMatchSell(source, itemId, collection)
            is OrderBidActivityDto -> convertOrderBid(source, itemId, collection)
            is OrderListActivityDto -> convertOrderList(source, itemId, collection)
            is OrderCancelBidActivityDto -> convertOrderCancelBid(source, itemId, collection)
            is OrderCancelListActivityDto -> convertOrderCancelList(source, itemId, collection)
            is AuctionOpenActivityDto -> convertAuctionOpen(source, itemId, collection)
            is AuctionBidActivityDto -> convertAuctionBid(source, itemId, collection)
            is AuctionFinishActivityDto -> convertAuctionFinish(source, itemId, collection)
            is AuctionCancelActivityDto -> convertAuctionCancel(source, itemId, collection)
            is AuctionStartActivityDto -> convertAuctionStart(source, itemId, collection)
            is AuctionEndActivityDto -> convertAuctionEnd(source, itemId, collection)
            is L2WithdrawalActivityDto -> convertL2Withdrawal(source, itemId, collection)
            is L2DepositActivityDto -> convertL2Deposit(source, itemId, collection)
            is OrderMatchSwapDto -> null
        }
    }

    private fun extractItemId(source: ActivityDto): ItemIdDto? {
        return when (source) {
            is MintActivityDto -> source.itemId
            is BurnActivityDto -> source.itemId
            is TransferActivityDto -> source.itemId
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
            is OrderMatchSwapDto -> null
        }
    }

    private fun getCollection(source: ActivityDto, itemId: ItemIdDto?, collection: String?): String? {
        if (collection != null) return collection
        // TODO remove after the custom collection release
        val collectionFromItemId = itemId?.let { IdParser.extractContract(itemId) }
        return when (source) {
            is MintActivityDto -> source.collection?.value ?: collectionFromItemId
            is BurnActivityDto -> source.collection?.value ?: collectionFromItemId
            is L2WithdrawalActivityDto -> source.collection?.value ?: collectionFromItemId
            is L2DepositActivityDto -> source.collection?.value ?: collectionFromItemId
            is TransferActivityDto -> source.collection?.value ?: collectionFromItemId
            is OrderMatchSellDto -> source.nft.type.ext.collectionId?.value
            is OrderBidActivityDto -> source.take.type.ext.collectionId?.value
            is OrderListActivityDto -> source.make.type.ext.collectionId?.value
            is OrderCancelBidActivityDto -> source.take.ext.collectionId?.value
            is OrderCancelListActivityDto -> source.make.ext.collectionId?.value
            is AuctionOpenActivityDto -> source.auction.sell.type.ext.collectionId?.value
            is AuctionBidActivityDto -> source.auction.sell.type.ext.collectionId?.value
            is AuctionFinishActivityDto -> source.auction.sell.type.ext.collectionId?.value
            is AuctionCancelActivityDto -> source.auction.sell.type.ext.collectionId?.value
            is AuctionStartActivityDto -> source.auction.sell.type.ext.collectionId?.value
            is AuctionEndActivityDto -> source.auction.sell.type.ext.collectionId?.value
            is OrderMatchSwapDto -> null
        }
    }

    private fun convertMint(source: MintActivityDto, itemId: ItemIdDto?, collection: String?): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date.truncatedToSeconds(),
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.MINT,
            userTo = source.owner.value,
            userFrom = null,
            collection = getCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertBurn(source: BurnActivityDto, itemId: ItemIdDto?, collection: String?): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date.truncatedToSeconds(),
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.BURN,
            userFrom = source.owner.value,
            userTo = null,
            collection = getCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertTransfer(source: TransferActivityDto, itemId: ItemIdDto?, collection: String?): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date.truncatedToSeconds(),
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.TRANSFER,
            userFrom = source.from.value,
            userTo = source.owner.value,
            collection = getCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private suspend fun convertOrderMatchSell(
        source: OrderMatchSellDto,
        itemId: ItemIdDto?,
        collection: String?
    ): EsActivity {
        val volumeInfo = sellActivityEnricher.provideVolumeInfo(source)
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date.truncatedToSeconds(),
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.SELL,
            userFrom = source.seller.value,
            userTo = source.buyer.value,
            collection = getCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
            currency = volumeInfo.sellCurrency,
            volumeUsd = volumeInfo.volumeUsd,
            volumeSell = volumeInfo.volumeSell,
            volumeNative = volumeInfo.volumeNative,
        )
    }

    private fun convertOrderBid(source: OrderBidActivityDto, itemId: ItemIdDto?, collection: String?): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date.truncatedToSeconds(),
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.BID,
            userFrom = source.maker.value,
            userTo = null,
            collection = getCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
            currency = getCurrencyAddressOrNull(source.make),
        )
    }

    private fun convertOrderList(source: OrderListActivityDto, itemId: ItemIdDto?, collection: String?): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date.truncatedToSeconds(),
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.LIST,
            userFrom = source.maker.value,
            userTo = null,
            collection = getCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
            currency = getCurrencyAddressOrNull(source.take),
        )
    }

    private fun convertOrderCancelBid(
        source: OrderCancelBidActivityDto,
        itemId: ItemIdDto?,
        collection: String?
    ): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date.truncatedToSeconds(),
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.CANCEL_BID,
            userFrom = source.maker.value,
            userTo = null,
            collection = getCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
            currency = getCurrencyAddressOrNull(source.make),
        )
    }

    private fun convertOrderCancelList(
        source: OrderCancelListActivityDto,
        itemId: ItemIdDto?,
        collection: String?
    ): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date.truncatedToSeconds(),
            blockNumber = source.blockchainInfo?.blockNumber,
            logIndex = source.blockchainInfo?.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.CANCEL_LIST,
            userFrom = source.maker.value,
            userTo = null,
            collection = getCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
            currency = getCurrencyAddressOrNull(source.take),
        )
    }

    private fun convertAuctionOpen(
        source: AuctionOpenActivityDto,
        itemId: ItemIdDto?,
        collection: String?
    ): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date.truncatedToSeconds(),
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_CREATED,
            userFrom = source.auction.seller.value,
            userTo = null,
            collection = getCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
            currency = getCurrencyAddressOrNull(source.auction.buy),
        )
    }

    private fun convertAuctionBid(source: AuctionBidActivityDto, itemId: ItemIdDto?, collection: String?): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date.truncatedToSeconds(),
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_BID,
            userFrom = source.auction.seller.value,
            userTo = source.bid.buyer.value,
            collection = getCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
            currency = getCurrencyAddressOrNull(source.auction.buy),
        )
    }

    private fun convertAuctionFinish(
        source: AuctionFinishActivityDto,
        itemId: ItemIdDto?,
        collection: String?
    ): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date.truncatedToSeconds(),
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_FINISHED,
            userFrom = source.auction.seller.value,
            userTo = null,
            collection = getCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
            currency = getCurrencyAddressOrNull(source.auction.buy),
        )
    }

    private fun convertAuctionCancel(
        source: AuctionCancelActivityDto,
        itemId: ItemIdDto?,
        collection: String?
    ): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date.truncatedToSeconds(),
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_CANCEL,
            userFrom = source.auction.seller.value,
            userTo = null,
            collection = getCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
            currency = getCurrencyAddressOrNull(source.auction.buy),
        )
    }

    private fun convertAuctionStart(
        source: AuctionStartActivityDto,
        itemId: ItemIdDto?,
        collection: String?
    ): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date.truncatedToSeconds(),
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_STARTED,
            userFrom = source.auction.seller.value,
            userTo = null,
            collection = getCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
            currency = getCurrencyAddressOrNull(source.auction.buy),
        )
    }

    private fun convertAuctionEnd(source: AuctionEndActivityDto, itemId: ItemIdDto?, collection: String?): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date.truncatedToSeconds(),
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.AUCTION_ENDED,
            userFrom = source.auction.seller.value,
            userTo = null,
            collection = getCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
            currency = getCurrencyAddressOrNull(source.auction.buy),
        )
    }

    private fun convertL2Withdrawal(
        source: L2WithdrawalActivityDto,
        itemId: ItemIdDto?,
        collection: String?
    ): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date.truncatedToSeconds(),
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.TRANSFER, // TODO
            userFrom = null,
            userTo = source.user.value,
            collection = getCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }

    private fun convertL2Deposit(source: L2DepositActivityDto, itemId: ItemIdDto?, collection: String?): EsActivity {
        return EsActivity(
            activityId = source.id.toString(),
            date = source.date.truncatedToSeconds(),
            blockNumber = null,
            logIndex = null,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.TRANSFER, // TODO
            userFrom = source.user.value,
            userTo = null,
            collection = getCollection(source, itemId, collection),
            item = itemId?.value.orEmpty(),
        )
    }
}
