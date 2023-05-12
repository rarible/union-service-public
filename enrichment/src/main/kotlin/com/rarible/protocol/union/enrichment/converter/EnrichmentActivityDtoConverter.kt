package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
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
import com.rarible.protocol.union.dto.OrderActivityMatchSideDto
import com.rarible.protocol.union.dto.OrderBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelListActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.OrderMatchSwapDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.converter.data.EnrichmentAssetData
import com.rarible.protocol.union.enrichment.model.EnrichmentActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentAuctionBidActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentAuctionCancelActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentAuctionEndActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentAuctionFinishActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentAuctionOpenActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentAuctionStartActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentBurnActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentL2DepositActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentL2WithdrawalActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentMintActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentOrderActivityMatchSide
import com.rarible.protocol.union.enrichment.model.EnrichmentOrderBidActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentOrderCancelBidActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentOrderCancelListActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentOrderListActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentOrderMatchSell
import com.rarible.protocol.union.enrichment.model.EnrichmentOrderMatchSwap
import com.rarible.protocol.union.enrichment.model.EnrichmentTransferActivity

@Suppress("UNUSED_PARAMETER")
object EnrichmentActivityDtoConverter {

    fun convert(
        source: EnrichmentActivity,
        cursor: String? = null,
        reverted: Boolean = false,
    ): ActivityDto {
        return when (source) {
            is EnrichmentMintActivity -> convert(source, cursor, reverted)
            is EnrichmentBurnActivity -> convert(source, cursor, reverted)
            is EnrichmentTransferActivity -> convert(source, cursor, reverted)
            is EnrichmentOrderMatchSwap -> convert(source, cursor, reverted)
            is EnrichmentOrderMatchSell -> convert(source, cursor, reverted)
            is EnrichmentOrderBidActivity -> convert(source, cursor, reverted)
            is EnrichmentOrderListActivity -> convert(source, cursor, reverted)
            is EnrichmentOrderCancelBidActivity -> convert(source, cursor, reverted)
            is EnrichmentOrderCancelListActivity -> convert(source, cursor, reverted)
            is EnrichmentAuctionOpenActivity -> convert(source, cursor, reverted)
            is EnrichmentAuctionBidActivity -> convert(source, cursor, reverted)
            is EnrichmentAuctionFinishActivity -> convert(source, cursor, reverted)
            is EnrichmentAuctionCancelActivity -> convert(source, cursor, reverted)
            is EnrichmentAuctionStartActivity -> convert(source, cursor, reverted)
            is EnrichmentAuctionEndActivity -> convert(source, cursor, reverted)
            is EnrichmentL2DepositActivity -> convert(source, cursor, reverted)
            is EnrichmentL2WithdrawalActivity -> convert(source, cursor, reverted)
        }
    }

    private fun convert(source: EnrichmentMintActivity, cursor: String?, reverted: Boolean): MintActivityDto {
        return MintActivityDto(
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            owner = source.owner,
            contract = source.contract,
            collection = source.collection?.let { IdParser.parseCollectionId(it) },
            tokenId = source.tokenId,
            itemId = source.itemId.let { IdParser.parseItemId(source.itemId) },
            value = source.value,
            mintPrice = source.mintPrice,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo
        )
    }

    private fun convert(source: EnrichmentBurnActivity, cursor: String?, reverted: Boolean): BurnActivityDto {
        return BurnActivityDto(
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            owner = source.owner,
            contract = source.contract,
            collection = source.collection?.let { IdParser.parseCollectionId(it) },
            tokenId = source.tokenId,
            itemId = source.itemId.let { IdParser.parseItemId(source.itemId) },
            value = source.value,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo
        )
    }

    private fun convert(source: EnrichmentTransferActivity, cursor: String?, reverted: Boolean): TransferActivityDto {
        return TransferActivityDto(
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            from = source.from,
            owner = source.owner,
            contract = source.contract,
            collection = source.collection?.let { IdParser.parseCollectionId(it) },
            tokenId = source.tokenId,
            itemId = source.itemId.let { IdParser.parseItemId(source.itemId) },
            value = source.value,
            purchase = source.purchase,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo
        )
    }

    private fun convert(source: EnrichmentOrderMatchSwap, cursor: String?, reverted: Boolean): OrderMatchSwapDto {
        return OrderMatchSwapDto(
            orderId = source.orderId,
            source = source.source,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo,
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            left = convert(source.left, source),
            right = convert(source.right, source)
        )
    }

    private fun convert(source: EnrichmentOrderMatchSell, cursor: String?, reverted: Boolean): OrderMatchSellDto {
        return OrderMatchSellDto(
            orderId = source.orderId,
            source = source.source,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo,
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            nft = convert(source.nft, source),
            payment = convert(source.payment, source),
            buyer = source.buyer,
            seller = source.seller,
            buyerOrderHash = source.buyerOrderHash,
            sellerOrderHash = source.sellerOrderHash,
            price = source.price,
            priceUsd = source.priceUsd,
            amountUsd = source.amountUsd,
            type = convert(source.type),
            sellMarketplaceMarker = source.sellMarketplaceMarker,
            buyMarketplaceMarker = source.buyMarketplaceMarker
        )
    }

    private fun convert(source: EnrichmentOrderBidActivity, cursor: String?, reverted: Boolean): OrderBidActivityDto {
        return OrderBidActivityDto(
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            orderId = source.orderId,
            hash = source.hash,
            maker = source.maker,
            make = convert(source.make, source),
            take = convert(source.take, source),
            price = source.price,
            priceUsd = source.priceUsd,
            source = source.source,
            marketplaceMarker = source.marketplaceMarker
        )
    }

    private fun convert(source: EnrichmentOrderListActivity, cursor: String?, reverted: Boolean): OrderListActivityDto {
        return OrderListActivityDto(
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            orderId = source.orderId,
            hash = source.hash,
            maker = source.maker,
            make = convert(source.make, source),
            take = convert(source.take, source),
            price = source.price,
            priceUsd = source.priceUsd,
            source = source.source
        )
    }

    private fun convert(
        source: EnrichmentOrderCancelBidActivity,
        cursor: String?,
        reverted: Boolean
    ): OrderCancelBidActivityDto {
        return OrderCancelBidActivityDto(
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            orderId = source.orderId,
            hash = source.hash,
            maker = source.maker,
            make = convert(source.make, source),
            take = convert(source.take, source),
            source = source.source,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo
        )
    }

    private fun convert(
        source: EnrichmentOrderCancelListActivity,
        cursor: String?,
        reverted: Boolean
    ): OrderCancelListActivityDto {
        return OrderCancelListActivityDto(
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            orderId = source.orderId,
            hash = source.hash,
            maker = source.maker,
            make = convert(source.make, source),
            take = convert(source.take, source),
            source = source.source,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo
        )
    }

    private fun convert(
        source: EnrichmentAuctionOpenActivity,
        cursor: String?,
        reverted: Boolean
    ): AuctionOpenActivityDto {
        return AuctionOpenActivityDto(
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            auction = source.auction,
            transactionHash = source.transactionHash,
        )
    }

    private fun convert(
        source: EnrichmentAuctionBidActivity,
        cursor: String?,
        reverted: Boolean
    ): AuctionBidActivityDto {
        return AuctionBidActivityDto(
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            auction = source.auction,
            bid = source.bid,
            transactionHash = source.transactionHash
        )
    }

    private fun convert(
        source: EnrichmentAuctionFinishActivity,
        cursor: String?,
        reverted: Boolean
    ): AuctionFinishActivityDto {
        return AuctionFinishActivityDto(
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            auction = source.auction,
            transactionHash = source.transactionHash,
        )
    }

    private fun convert(
        source: EnrichmentAuctionCancelActivity,
        cursor: String?,
        reverted: Boolean
    ): AuctionCancelActivityDto {
        return AuctionCancelActivityDto(
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            auction = source.auction,
            transactionHash = source.transactionHash,
        )
    }

    private fun convert(
        source: EnrichmentAuctionStartActivity,
        cursor: String?,
        reverted: Boolean
    ): AuctionStartActivityDto {
        return AuctionStartActivityDto(
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            auction = source.auction,
        )
    }

    private fun convert(
        source: EnrichmentAuctionEndActivity,
        cursor: String?,
        reverted: Boolean
    ): AuctionEndActivityDto {
        return AuctionEndActivityDto(
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            auction = source.auction,
        )
    }

    private fun convert(source: EnrichmentL2DepositActivity, cursor: String?, reverted: Boolean): L2DepositActivityDto {
        return L2DepositActivityDto(
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            user = source.user,
            status = source.status,
            itemId = source.itemId.let { IdParser.parseItemId(source.itemId) },
            value = source.value,
            collection = source.collection?.let { IdParser.parseCollectionId(it) },
        )
    }

    private fun convert(
        source: EnrichmentL2WithdrawalActivity,
        cursor: String?,
        reverted: Boolean
    ): L2WithdrawalActivityDto {
        return L2WithdrawalActivityDto(
            id = source.id.toDto(),
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = cursor,
            reverted = reverted,
            user = source.user,
            status = source.status,
            itemId = source.itemId.let { IdParser.parseItemId(source.itemId) },
            collection = source.collection?.let { IdParser.parseCollectionId(it) },
            value = source.value,
        )
    }

    private fun convert(source: EnrichmentOrderMatchSell.Type): OrderMatchSellDto.Type {
        return when (source) {
            EnrichmentOrderMatchSell.Type.SELL -> OrderMatchSellDto.Type.SELL
            EnrichmentOrderMatchSell.Type.ACCEPT_BID -> OrderMatchSellDto.Type.ACCEPT_BID
        }
    }

    private fun convert(
        source: EnrichmentOrderActivityMatchSide,
        activity: EnrichmentActivity
    ): OrderActivityMatchSideDto {
        return OrderActivityMatchSideDto(
            maker = source.maker,
            hash = source.hash,
            asset = convert(source.asset, activity)
        )
    }

    private fun convert(
        source: UnionAsset,
        activity: EnrichmentActivity
    ): AssetDto {
        return AssetDto(
            type = convert(source.type, activity),
            value = source.value
        )
    }

    private fun convert(
        source: UnionAssetType,
        activity: EnrichmentActivity
    ): AssetTypeDto {
        return AssetDtoConverter.convert(
            source,
            EnrichmentAssetData(activity.collection?.let { IdParser.parseCollectionId(it) })
        )
    }
}