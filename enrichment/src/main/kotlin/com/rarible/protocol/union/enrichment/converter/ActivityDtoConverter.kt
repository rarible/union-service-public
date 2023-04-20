package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionActivityDto
import com.rarible.protocol.union.core.model.UnionAssetDto
import com.rarible.protocol.union.core.model.UnionAssetTypeDto
import com.rarible.protocol.union.core.model.UnionAuctionBidActivityDto
import com.rarible.protocol.union.core.model.UnionAuctionCancelActivityDto
import com.rarible.protocol.union.core.model.UnionAuctionEndActivityDto
import com.rarible.protocol.union.core.model.UnionAuctionFinishActivityDto
import com.rarible.protocol.union.core.model.UnionAuctionOpenActivityDto
import com.rarible.protocol.union.core.model.UnionAuctionStartActivityDto
import com.rarible.protocol.union.core.model.UnionBurnActivityDto
import com.rarible.protocol.union.core.model.UnionL2DepositActivityDto
import com.rarible.protocol.union.core.model.UnionL2WithdrawalActivityDto
import com.rarible.protocol.union.core.model.UnionMintActivityDto
import com.rarible.protocol.union.core.model.UnionOrderActivityMatchSideDto
import com.rarible.protocol.union.core.model.UnionOrderBidActivityDto
import com.rarible.protocol.union.core.model.UnionOrderCancelBidActivityDto
import com.rarible.protocol.union.core.model.UnionOrderCancelListActivityDto
import com.rarible.protocol.union.core.model.UnionOrderListActivityDto
import com.rarible.protocol.union.core.model.UnionOrderMatchSellDto
import com.rarible.protocol.union.core.model.UnionOrderMatchSwapDto
import com.rarible.protocol.union.core.model.UnionTransferActivityDto
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
        source: UnionActivityDto,
        data: EnrichmentActivityData = EnrichmentActivityData.empty()
    ): ActivityDto {
        return when (source) {
            is UnionMintActivityDto -> convert(source, data)
            is UnionBurnActivityDto -> convert(source, data)
            is UnionTransferActivityDto -> convert(source, data)
            is UnionOrderMatchSwapDto -> convert(source, data)
            is UnionOrderMatchSellDto -> convert(source, data)
            is UnionOrderBidActivityDto -> convert(source, data)
            is UnionOrderListActivityDto -> convert(source, data)
            is UnionOrderCancelBidActivityDto -> convert(source, data)
            is UnionOrderCancelListActivityDto -> convert(source, data)
            is UnionAuctionOpenActivityDto -> convert(source, data)
            is UnionAuctionBidActivityDto -> convert(source, data)
            is UnionAuctionFinishActivityDto -> convert(source, data)
            is UnionAuctionCancelActivityDto -> convert(source, data)
            is UnionAuctionStartActivityDto -> convert(source, data)
            is UnionAuctionEndActivityDto -> convert(source, data)
            is UnionL2DepositActivityDto -> convert(source, data)
            is UnionL2WithdrawalActivityDto -> convert(source, data)
        }
    }

    private fun convert(source: UnionMintActivityDto, data: EnrichmentActivityData): MintActivityDto {
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

    private fun convert(source: UnionBurnActivityDto, data: EnrichmentActivityData): BurnActivityDto {
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

    private fun convert(source: UnionTransferActivityDto, data: EnrichmentActivityData): TransferActivityDto {
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

    private fun convert(source: UnionOrderMatchSwapDto, data: EnrichmentActivityData): OrderMatchSwapDto {
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
            left = convert(source.left, data),
            right = convert(source.right, data)
        )
    }

    private fun convert(source: UnionOrderMatchSellDto, data: EnrichmentActivityData): OrderMatchSellDto {
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
            nft = convert(source.nft, data),
            payment = convert(source.payment, data),
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

    private fun convert(source: UnionOrderBidActivityDto, data: EnrichmentActivityData): OrderBidActivityDto {
        return OrderBidActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            orderId = source.orderId,
            hash = source.hash,
            maker = source.maker,
            make = convert(source.make, data),
            take = convert(source.take, data),
            price = source.price,
            priceUsd = source.priceUsd,
            source = source.source,
            marketplaceMarker = source.marketplaceMarker
        )
    }

    private fun convert(source: UnionOrderListActivityDto, data: EnrichmentActivityData): OrderListActivityDto {
        return OrderListActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            orderId = source.orderId,
            hash = source.hash,
            maker = source.maker,
            make = convert(source.make, data),
            take = convert(source.take, data),
            price = source.price,
            priceUsd = source.priceUsd,
            source = source.source
        )
    }

    private fun convert(
        source: UnionOrderCancelBidActivityDto,
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
            make = convert(source.make, data),
            take = convert(source.take, data),
            source = source.source,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo
        )
    }

    private fun convert(
        source: UnionOrderCancelListActivityDto,
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
            make = convert(source.make, data),
            take = convert(source.take, data),
            source = source.source,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo
        )
    }

    private fun convert(source: UnionAuctionOpenActivityDto, data: EnrichmentActivityData): AuctionOpenActivityDto {
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

    private fun convert(source: UnionAuctionBidActivityDto, data: EnrichmentActivityData): AuctionBidActivityDto {
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

    private fun convert(source: UnionAuctionFinishActivityDto, data: EnrichmentActivityData): AuctionFinishActivityDto {
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

    private fun convert(source: UnionAuctionCancelActivityDto, data: EnrichmentActivityData): AuctionCancelActivityDto {
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

    private fun convert(source: UnionAuctionStartActivityDto, data: EnrichmentActivityData): AuctionStartActivityDto {
        return AuctionStartActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            auction = source.auction,
        )

    }

    private fun convert(source: UnionAuctionEndActivityDto, data: EnrichmentActivityData): AuctionEndActivityDto {
        return AuctionEndActivityDto(
            id = source.id,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            cursor = source.cursor,
            reverted = source.reverted,
            auction = source.auction,
        )

    }

    private fun convert(source: UnionL2DepositActivityDto, data: EnrichmentActivityData): L2DepositActivityDto {
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

    private fun convert(source: UnionL2WithdrawalActivityDto, data: EnrichmentActivityData): L2WithdrawalActivityDto {
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

    private fun convert(source: UnionOrderMatchSellDto.Type): OrderMatchSellDto.Type {
        return when (source) {
            UnionOrderMatchSellDto.Type.SELL -> OrderMatchSellDto.Type.SELL
            UnionOrderMatchSellDto.Type.ACCEPT_BID -> OrderMatchSellDto.Type.ACCEPT_BID
        }
    }

    private fun convert(
        source: UnionOrderActivityMatchSideDto,
        data: EnrichmentActivityData
    ): OrderActivityMatchSideDto {
        return OrderActivityMatchSideDto(
            maker = source.maker,
            hash = source.hash,
            asset = convert(source.asset, data)
        )
    }

    fun convert(
        source: UnionAssetDto,
        data: EnrichmentActivityData
    ): AssetDto {
        return AssetDto(
            type = convert(source.type, data),
            value = source.value
        )
    }

    fun convert(
        source: UnionAssetTypeDto,
        data: EnrichmentActivityData
    ): AssetTypeDto {
        return AssetDtoConverter.convert(source, EnrichmentAssetData(data.customCollection))
    }

    private fun UnionActivityDto.getEnrichedCollection(data: EnrichmentActivityData): CollectionIdDto? {
        return data.customCollection ?: this.collectionId()
    }

}