package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.core.model.UnionAuctionBidActivity
import com.rarible.protocol.union.core.model.UnionAuctionCancelActivity
import com.rarible.protocol.union.core.model.UnionAuctionEndActivity
import com.rarible.protocol.union.core.model.UnionAuctionFinishActivity
import com.rarible.protocol.union.core.model.UnionAuctionOpenActivity
import com.rarible.protocol.union.core.model.UnionAuctionStartActivity
import com.rarible.protocol.union.core.model.UnionBurnActivity
import com.rarible.protocol.union.core.model.UnionL2DepositActivity
import com.rarible.protocol.union.core.model.UnionL2WithdrawalActivity
import com.rarible.protocol.union.core.model.UnionMintActivity
import com.rarible.protocol.union.core.model.UnionOrderActivityMatchSideDto
import com.rarible.protocol.union.core.model.UnionOrderBidActivity
import com.rarible.protocol.union.core.model.UnionOrderCancelBidActivity
import com.rarible.protocol.union.core.model.UnionOrderCancelListActivity
import com.rarible.protocol.union.core.model.UnionOrderListActivity
import com.rarible.protocol.union.core.model.UnionOrderMatchSell
import com.rarible.protocol.union.core.model.UnionOrderMatchSwap
import com.rarible.protocol.union.core.model.UnionTransferActivity
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.AuctionBidActivityDto
import com.rarible.protocol.union.dto.AuctionCancelActivityDto
import com.rarible.protocol.union.dto.AuctionEndActivityDto
import com.rarible.protocol.union.dto.AuctionFinishActivityDto
import com.rarible.protocol.union.dto.AuctionOpenActivityDto
import com.rarible.protocol.union.dto.AuctionStartActivityDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.CollectionIdDto
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
import com.rarible.protocol.union.enrichment.converter.data.EnrichmentActivityData
import com.rarible.protocol.union.enrichment.converter.data.EnrichmentAssetData

@Suppress("UNUSED_PARAMETER")
object ActivityDtoConverter {

    fun convert(
        source: UnionActivity,
        data: EnrichmentActivityData = EnrichmentActivityData.empty()
    ): ActivityDto {
        return when (source) {
            is UnionMintActivity -> convert(source, data)
            is UnionBurnActivity -> convert(source, data)
            is UnionTransferActivity -> convert(source, data)
            is UnionOrderMatchSwap -> convert(source, data)
            is UnionOrderMatchSell -> convert(source, data)
            is UnionOrderBidActivity -> convert(source, data)
            is UnionOrderListActivity -> convert(source, data)
            is UnionOrderCancelBidActivity -> convert(source, data)
            is UnionOrderCancelListActivity -> convert(source, data)
            is UnionAuctionOpenActivity -> convert(source, data)
            is UnionAuctionBidActivity -> convert(source, data)
            is UnionAuctionFinishActivity -> convert(source, data)
            is UnionAuctionCancelActivity -> convert(source, data)
            is UnionAuctionStartActivity -> convert(source, data)
            is UnionAuctionEndActivity -> convert(source, data)
            is UnionL2DepositActivity -> convert(source, data)
            is UnionL2WithdrawalActivity -> convert(source, data)
        }
    }

    private fun convert(source: UnionMintActivity, data: EnrichmentActivityData): MintActivityDto {
        return MintActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            owner = source.owner,
            contract = source.contract,
            collection = source.getEnrichedCollection(data),
            tokenId = source.tokenId,
            itemId = source.itemId,
            value = source.value,
            mintPrice = source.mintPrice,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo
        )
    }

    private fun convert(source: UnionBurnActivity, data: EnrichmentActivityData): BurnActivityDto {
        return BurnActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            owner = source.owner,
            contract = source.contract,
            collection = source.getEnrichedCollection(data),
            tokenId = source.tokenId,
            itemId = source.itemId,
            value = source.value,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo
        )
    }

    private fun convert(source: UnionTransferActivity, data: EnrichmentActivityData): TransferActivityDto {
        return TransferActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            from = source.from,
            owner = source.owner,
            contract = source.contract,
            collection = source.getEnrichedCollection(data),
            tokenId = source.tokenId,
            itemId = source.itemId,
            value = source.value,
            purchase = source.purchase,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo
        )
    }

    private fun convert(source: UnionOrderMatchSwap, data: EnrichmentActivityData): OrderMatchSwapDto {
        return OrderMatchSwapDto(
            orderId = source.orderId,
            source = source.source,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo,
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            left = convert(source.id, source.left, data),
            right = convert(source.id, source.right, data)
        )
    }

    private fun convert(source: UnionOrderMatchSell, data: EnrichmentActivityData): OrderMatchSellDto {
        return OrderMatchSellDto(
            orderId = source.orderId,
            source = source.source,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo,
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            nft = convert(source.id, source.nft, data),
            payment = convert(source.id, source.payment, data),
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

    private fun convert(source: UnionOrderBidActivity, data: EnrichmentActivityData): OrderBidActivityDto {
        return OrderBidActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            orderId = source.orderId,
            hash = source.hash,
            maker = source.maker,
            make = convert(source.id, source.make, data),
            take = convert(source.id, source.take, data),
            price = source.price,
            priceUsd = source.priceUsd,
            source = source.source,
            marketplaceMarker = source.marketplaceMarker
        )
    }

    private fun convert(source: UnionOrderListActivity, data: EnrichmentActivityData): OrderListActivityDto {
        return OrderListActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            orderId = source.orderId,
            hash = source.hash,
            maker = source.maker,
            make = convert(source.id, source.make, data),
            take = convert(source.id, source.take, data),
            price = source.price,
            priceUsd = source.priceUsd,
            source = source.source
        )
    }

    private fun convert(
        source: UnionOrderCancelBidActivity,
        data: EnrichmentActivityData
    ): OrderCancelBidActivityDto {
        return OrderCancelBidActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            orderId = source.orderId,
            hash = source.hash,
            maker = source.maker,
            make = convert(source.id, source.make, data),
            take = convert(source.id, source.take, data),
            source = source.source,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo
        )
    }

    private fun convert(
        source: UnionOrderCancelListActivity,
        data: EnrichmentActivityData
    ): OrderCancelListActivityDto {
        return OrderCancelListActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            orderId = source.orderId,
            hash = source.hash,
            maker = source.maker,
            make = convert(source.id, source.make, data),
            take = convert(source.id, source.take, data),
            source = source.source,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo
        )
    }

    private fun convert(source: UnionAuctionOpenActivity, data: EnrichmentActivityData): AuctionOpenActivityDto {
        return AuctionOpenActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            auction = source.auction,
            transactionHash = source.transactionHash,
        )

    }

    private fun convert(source: UnionAuctionBidActivity, data: EnrichmentActivityData): AuctionBidActivityDto {
        return AuctionBidActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            auction = source.auction,
            bid = source.bid,
            transactionHash = source.transactionHash
        )
    }

    private fun convert(source: UnionAuctionFinishActivity, data: EnrichmentActivityData): AuctionFinishActivityDto {
        return AuctionFinishActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            auction = source.auction,
            transactionHash = source.transactionHash,
        )
    }

    private fun convert(source: UnionAuctionCancelActivity, data: EnrichmentActivityData): AuctionCancelActivityDto {
        return AuctionCancelActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            auction = source.auction,
            transactionHash = source.transactionHash,
        )

    }

    private fun convert(source: UnionAuctionStartActivity, data: EnrichmentActivityData): AuctionStartActivityDto {
        return AuctionStartActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            auction = source.auction,
        )

    }

    private fun convert(source: UnionAuctionEndActivity, data: EnrichmentActivityData): AuctionEndActivityDto {
        return AuctionEndActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            auction = source.auction,
        )

    }

    private fun convert(source: UnionL2DepositActivity, data: EnrichmentActivityData): L2DepositActivityDto {
        return L2DepositActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            user = source.user,
            status = source.status,
            itemId = source.itemId,
            value = source.value,
            collection = source.getEnrichedCollection(data),
        )
    }

    private fun convert(source: UnionL2WithdrawalActivity, data: EnrichmentActivityData): L2WithdrawalActivityDto {
        return L2WithdrawalActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            user = source.user,
            status = source.status,
            itemId = source.itemId,
            collection = source.getEnrichedCollection(data),
            value = source.value,
        )
    }

    private fun convert(source: UnionOrderMatchSell.Type): OrderMatchSellDto.Type {
        return when (source) {
            UnionOrderMatchSell.Type.SELL -> OrderMatchSellDto.Type.SELL
            UnionOrderMatchSell.Type.ACCEPT_BID -> OrderMatchSellDto.Type.ACCEPT_BID
        }
    }

    private fun convert(
        id: ActivityIdDto,
        source: UnionOrderActivityMatchSideDto,
        data: EnrichmentActivityData
    ): OrderActivityMatchSideDto {
        return OrderActivityMatchSideDto(
            maker = source.maker,
            hash = source.hash,
            asset = convert(id, source.asset, data)
        )
    }

    private fun convert(
        id: ActivityIdDto,
        source: UnionAsset,
        data: EnrichmentActivityData
    ): AssetDto {
        return AssetDto(
            type = convert(id, source.type, data),
            value = source.value
        )
    }

    private fun convert(
        id: ActivityIdDto,
        source: UnionAssetType,
        data: EnrichmentActivityData
    ): AssetTypeDto {
        return AssetDtoConverter.convert(source, EnrichmentAssetData(data.customCollections[id]))
    }

    private fun UnionActivity.getEnrichedCollection(data: EnrichmentActivityData): CollectionIdDto? {
        return data.customCollections[id] ?: this.collectionId()
    }

}