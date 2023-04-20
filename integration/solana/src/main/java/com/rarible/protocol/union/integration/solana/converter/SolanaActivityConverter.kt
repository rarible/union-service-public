package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.solana.dto.ActivityFilterAllTypeDto
import com.rarible.protocol.solana.dto.ActivityFilterByCollectionTypeDto
import com.rarible.protocol.solana.dto.ActivityFilterByItemTypeDto
import com.rarible.protocol.solana.dto.ActivityFilterByUserTypeDto
import com.rarible.protocol.solana.dto.OrderMatchActivityDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionBurnActivity
import com.rarible.protocol.union.core.model.UnionMintActivity
import com.rarible.protocol.union.core.model.UnionOrderActivityMatchSideDto
import com.rarible.protocol.union.core.model.UnionOrderBidActivity
import com.rarible.protocol.union.core.model.UnionOrderCancelBidActivity
import com.rarible.protocol.union.core.model.UnionOrderCancelListActivity
import com.rarible.protocol.union.core.model.UnionOrderListActivity
import com.rarible.protocol.union.core.model.UnionOrderMatchSell
import com.rarible.protocol.union.core.model.UnionOrderMatchSwap
import com.rarible.protocol.union.core.model.UnionTransferActivity
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SolanaActivityConverter(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(
        source: com.rarible.protocol.solana.dto.ActivityDto,
        blockchain: BlockchainDto
    ): UnionActivity {
        val activityId = ActivityIdDto(blockchain, source.id)
        val activitySource = OrderActivitySourceDto.RARIBLE
        return when (source) {
            is OrderMatchActivityDto -> {
                val type = source.type

                val nft = SolanaConverter.convert(source.nft, blockchain)
                val payment = SolanaConverter.convert(source.payment, blockchain)

                val nftType = nft.type
                val paymentType = payment.type

                val blockchainInfo = convert(source.blockchainInfo)

                val seller = UnionAddressConverter.convert(blockchain, source.seller)
                val buyer = UnionAddressConverter.convert(blockchain, source.buyer)

                if (nftType.isNft() && paymentType.isCurrency()) {
                    val priceUsd = currencyService.toUsd(blockchain, payment.type, source.price, source.date)
                    UnionOrderMatchSell(
                        id = activityId,
                        date = source.date,
                        source = activitySource,
                        transactionHash = blockchainInfo!!.transactionHash, // TODO must be not null
                        nft = nft,
                        payment = payment,
                        seller = seller,
                        buyer = buyer,
                        sellerOrderHash = source.sellerOrderHash,
                        buyerOrderHash = source.buyerOrderHash,
                        price = source.price,
                        priceUsd = priceUsd,
                        amountUsd = priceUsd?.multiply(nft.value),
                        type = convert(type),
                        reverted = source.reverted,
                        // TODO UNION remove in 1.19
                        blockchainInfo = blockchainInfo,
                        lastUpdatedAt = source.dbUpdatedAt
                    )
                } else {
                    if (paymentType.isNft() || nftType.isCurrency()) {
                        logger.warn(
                            "Incorrect SOLANA OrderMatch activity, nft and payment are mixed up: {}",
                            source
                        )
                    }
                    // Originally we should NOT receive such events
                    val sellSide = UnionOrderActivityMatchSideDto(seller, source.sellerOrderHash, nft)
                    val buySide = UnionOrderActivityMatchSideDto(buyer, source.buyerOrderHash, payment)

                    UnionOrderMatchSwap(
                        id = activityId,
                        date = source.date,
                        source = activitySource,
                        transactionHash = source.blockchainInfo.transactionHash,
                        left = if (type == OrderMatchActivityDto.Type.SELL) sellSide else buySide,
                        right = if (type == OrderMatchActivityDto.Type.ACCEPT_BID) sellSide else buySide,
                        reverted = source.reverted,
                        // TODO UNION remove in 1.19
                        blockchainInfo = blockchainInfo,
                        lastUpdatedAt = source.dbUpdatedAt
                    )
                }
            }
            is com.rarible.protocol.solana.dto.OrderBidActivityDto -> {
                val payment = SolanaConverter.convert(source.make, blockchain)
                val nft = SolanaConverter.convert(source.take, blockchain)
                val priceUsd = currencyService.toUsd(blockchain, payment.type, source.price, source.date)

                UnionOrderBidActivity(
                    id = activityId,
                    date = source.date,
                    price = source.price,
                    priceUsd = priceUsd,
                    source = activitySource,
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(blockchain, source.maker),
                    make = payment,
                    take = nft,
                    reverted = source.reverted,
                    lastUpdatedAt = source.dbUpdatedAt
                )
            }
            is com.rarible.protocol.solana.dto.OrderListActivityDto -> {
                val payment = SolanaConverter.convert(source.take, blockchain)
                val nft = SolanaConverter.convert(source.make, blockchain)
                val priceUsd = currencyService.toUsd(blockchain, payment.type, source.price, source.date)

                UnionOrderListActivity(
                    id = activityId,
                    date = source.date,
                    price = source.price,
                    priceUsd = priceUsd,
                    source = activitySource,
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(blockchain, source.maker),
                    make = nft,
                    take = payment,
                    reverted = source.reverted,
                    lastUpdatedAt = source.dbUpdatedAt
                )
            }
            is com.rarible.protocol.solana.dto.OrderCancelBidActivityDto -> {
                UnionOrderCancelBidActivity(
                    id = activityId,
                    date = source.date,
                    source = activitySource,
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(blockchain, source.maker),
                    make = SolanaConverter.convert(source.make, blockchain),
                    take = SolanaConverter.convert(source.take, blockchain),
                    transactionHash = source.blockchainInfo.transactionHash,
                    reverted = source.reverted,
                    // TODO UNION remove in 1.19
                    blockchainInfo = convert(source.blockchainInfo),
                    lastUpdatedAt = source.dbUpdatedAt
                )
            }
            is com.rarible.protocol.solana.dto.OrderCancelListActivityDto -> {
                UnionOrderCancelListActivity(
                    id = activityId,
                    date = source.date,
                    source = activitySource,
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(blockchain, source.maker),
                    make = SolanaConverter.convert(source.make, blockchain),
                    take = SolanaConverter.convert(source.take, blockchain),
                    transactionHash = source.blockchainInfo.transactionHash,
                    reverted = source.reverted,
                    // TODO UNION remove in 1.19
                    blockchainInfo = convert(source.blockchainInfo),
                    lastUpdatedAt = source.dbUpdatedAt
                )
            }
            is com.rarible.protocol.solana.dto.MintActivityDto -> {

                UnionMintActivity(
                    id = activityId,
                    date = source.date,
                    owner = UnionAddressConverter.convert(blockchain, source.owner),
                    itemId = ItemIdDto(blockchain, source.tokenAddress),
                    collection = null, // TODO solana should send collections with items
                    value = source.value,
                    transactionHash = source.blockchainInfo.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = convert(source.blockchainInfo),
                    reverted = source.reverted,
                    lastUpdatedAt = source.dbUpdatedAt,
                )
            }
            is com.rarible.protocol.solana.dto.BurnActivityDto -> {
                UnionBurnActivity(
                    id = activityId,
                    date = source.date,
                    owner = UnionAddressConverter.convert(blockchain, source.owner),
                    itemId = ItemIdDto(blockchain, source.tokenAddress),
                    collection = null, // TODO solana should send collections with items
                    value = source.value,
                    transactionHash = source.blockchainInfo.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = convert(source.blockchainInfo),
                    reverted = source.reverted,
                    lastUpdatedAt = source.dbUpdatedAt
                )
            }
            is com.rarible.protocol.solana.dto.TransferActivityDto -> {
                UnionTransferActivity(
                    id = activityId,
                    date = source.date,
                    from = UnionAddressConverter.convert(blockchain, source.from),
                    owner = UnionAddressConverter.convert(blockchain, source.owner),
                    itemId = ItemIdDto(blockchain, source.tokenAddress),
                    collection = null, // TODO solana should send collections with items
                    value = source.value,
                    purchase = source.purchase,
                    transactionHash = source.blockchainInfo.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = convert(source.blockchainInfo),
                    reverted = source.reverted,
                    lastUpdatedAt = source.dbUpdatedAt
                )
            }
        }
    }

    fun convertToAllTypes(type: ActivityTypeDto): ActivityFilterAllTypeDto? {
        return when (type) {
            ActivityTypeDto.TRANSFER -> ActivityFilterAllTypeDto.TRANSFER
            ActivityTypeDto.MINT -> ActivityFilterAllTypeDto.MINT
            ActivityTypeDto.BURN -> ActivityFilterAllTypeDto.BURN
            ActivityTypeDto.BID -> ActivityFilterAllTypeDto.BID
            ActivityTypeDto.LIST -> ActivityFilterAllTypeDto.LIST
            ActivityTypeDto.SELL -> ActivityFilterAllTypeDto.SELL
            ActivityTypeDto.CANCEL_LIST -> ActivityFilterAllTypeDto.CANCEL_LIST
            ActivityTypeDto.CANCEL_BID -> ActivityFilterAllTypeDto.CANCEL_BID
            ActivityTypeDto.AUCTION_BID -> null
            ActivityTypeDto.AUCTION_CREATED -> null
            ActivityTypeDto.AUCTION_CANCEL -> null
            ActivityTypeDto.AUCTION_FINISHED -> null
            ActivityTypeDto.AUCTION_STARTED -> null
            ActivityTypeDto.AUCTION_ENDED -> null
        }
    }

    fun convertToCollectionTypes(type: ActivityTypeDto): ActivityFilterByCollectionTypeDto? {
        return when (type) {
            ActivityTypeDto.TRANSFER -> ActivityFilterByCollectionTypeDto.TRANSFER
            ActivityTypeDto.MINT -> ActivityFilterByCollectionTypeDto.MINT
            ActivityTypeDto.BURN -> ActivityFilterByCollectionTypeDto.BURN
            ActivityTypeDto.BID -> ActivityFilterByCollectionTypeDto.BID
            ActivityTypeDto.LIST -> ActivityFilterByCollectionTypeDto.LIST
            ActivityTypeDto.SELL -> ActivityFilterByCollectionTypeDto.SELL
            ActivityTypeDto.CANCEL_LIST -> ActivityFilterByCollectionTypeDto.CANCEL_LIST
            ActivityTypeDto.CANCEL_BID -> ActivityFilterByCollectionTypeDto.CANCEL_BID
            ActivityTypeDto.AUCTION_BID -> null
            ActivityTypeDto.AUCTION_CREATED -> null
            ActivityTypeDto.AUCTION_CANCEL -> null
            ActivityTypeDto.AUCTION_FINISHED -> null
            ActivityTypeDto.AUCTION_STARTED -> null
            ActivityTypeDto.AUCTION_ENDED -> null
        }
    }

    fun convertToItemTypes(type: ActivityTypeDto): ActivityFilterByItemTypeDto? {
        return when (type) {
            ActivityTypeDto.TRANSFER -> ActivityFilterByItemTypeDto.TRANSFER
            ActivityTypeDto.MINT -> ActivityFilterByItemTypeDto.MINT
            ActivityTypeDto.BURN -> ActivityFilterByItemTypeDto.BURN
            ActivityTypeDto.BID -> ActivityFilterByItemTypeDto.BID
            ActivityTypeDto.LIST -> ActivityFilterByItemTypeDto.LIST
            ActivityTypeDto.SELL -> ActivityFilterByItemTypeDto.SELL
            ActivityTypeDto.CANCEL_LIST -> ActivityFilterByItemTypeDto.CANCEL_LIST
            ActivityTypeDto.CANCEL_BID -> ActivityFilterByItemTypeDto.CANCEL_BID
            ActivityTypeDto.AUCTION_BID -> null
            ActivityTypeDto.AUCTION_CREATED -> null
            ActivityTypeDto.AUCTION_CANCEL -> null
            ActivityTypeDto.AUCTION_FINISHED -> null
            ActivityTypeDto.AUCTION_STARTED -> null
            ActivityTypeDto.AUCTION_ENDED -> null
        }
    }

    fun convertToUserTypes(type: UserActivityTypeDto): ActivityFilterByUserTypeDto? {
        return when (type) {
            UserActivityTypeDto.TRANSFER_FROM -> ActivityFilterByUserTypeDto.TRANSFER_FROM
            UserActivityTypeDto.TRANSFER_TO -> ActivityFilterByUserTypeDto.TRANSFER_TO
            UserActivityTypeDto.MINT -> ActivityFilterByUserTypeDto.MINT
            UserActivityTypeDto.BURN -> ActivityFilterByUserTypeDto.BURN
            UserActivityTypeDto.MAKE_BID -> ActivityFilterByUserTypeDto.MAKE_BID
            UserActivityTypeDto.GET_BID -> ActivityFilterByUserTypeDto.GET_BID
            UserActivityTypeDto.LIST -> ActivityFilterByUserTypeDto.LIST
            UserActivityTypeDto.BUY -> ActivityFilterByUserTypeDto.BUY
            UserActivityTypeDto.SELL -> ActivityFilterByUserTypeDto.SELL
            UserActivityTypeDto.CANCEL_LIST -> ActivityFilterByUserTypeDto.CANCEL_LIST
            UserActivityTypeDto.CANCEL_BID -> ActivityFilterByUserTypeDto.CANCEL_BID
            UserActivityTypeDto.AUCTION_BID -> null
            UserActivityTypeDto.AUCTION_CREATED -> null
            UserActivityTypeDto.AUCTION_CANCEL -> null
            UserActivityTypeDto.AUCTION_FINISHED -> null
            UserActivityTypeDto.AUCTION_STARTED -> null
            UserActivityTypeDto.AUCTION_ENDED -> null
        }
    }

    private fun convert(
        source: com.rarible.protocol.solana.dto.ActivityBlockchainInfoDto?
    ): ActivityBlockchainInfoDto? {
        if (source == null) return null
        return ActivityBlockchainInfoDto(
            transactionHash = source.transactionHash,
            blockHash = source.blockHash,
            blockNumber = source.blockNumber,
            logIndex = source.transactionIndex
        )
    }

    private fun convert(source: OrderMatchActivityDto.Type) =
        when (source) {
            OrderMatchActivityDto.Type.SELL -> UnionOrderMatchSell.Type.SELL
            OrderMatchActivityDto.Type.ACCEPT_BID -> UnionOrderMatchSell.Type.ACCEPT_BID
        }

    fun convert(source: SyncSortDto): com.rarible.protocol.solana.dto.SyncSortDto =
        when (source) {
            SyncSortDto.DB_UPDATE_ASC -> com.rarible.protocol.solana.dto.SyncSortDto.DB_UPDATE_ASC
            SyncSortDto.DB_UPDATE_DESC -> com.rarible.protocol.solana.dto.SyncSortDto.DB_UPDATE_DESC
        }

    fun convert(source: SyncTypeDto): com.rarible.protocol.solana.dto.SyncTypeDto =
        when (source) {
            SyncTypeDto.ORDER -> com.rarible.protocol.solana.dto.SyncTypeDto.ORDER
            SyncTypeDto.NFT -> com.rarible.protocol.solana.dto.SyncTypeDto.NFT
            SyncTypeDto.AUCTION -> com.rarible.protocol.solana.dto.SyncTypeDto.AUCTION
        }
}