package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionActivity
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
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.converter.data.EnrichmentActivityData
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
object EnrichmentActivityConverter {

    fun convert(
        source: UnionActivity,
        data: EnrichmentActivityData = EnrichmentActivityData.empty()
    ): EnrichmentActivity {
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

    private fun convert(source: UnionMintActivity, data: EnrichmentActivityData): EnrichmentMintActivity {
        return EnrichmentMintActivity(
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            owner = source.owner,
            contract = source.contract,
            collection = source.getEnrichedCollection(data)?.fullId() ?: source.collection?.fullId(),
            tokenId = source.tokenId,
            itemId = source.itemId!!.fullId(),
            value = source.value,
            mintPrice = source.mintPrice,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo
        )
    }

    private fun convert(source: UnionBurnActivity, data: EnrichmentActivityData): EnrichmentBurnActivity {
        return EnrichmentBurnActivity(
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            owner = source.owner,
            contract = source.contract,
            collection = source.getEnrichedCollection(data)?.fullId() ?: source.collection?.fullId(),
            tokenId = source.tokenId,
            itemId = source.itemId!!.fullId(),
            value = source.value,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo
        )
    }

    private fun convert(source: UnionTransferActivity, data: EnrichmentActivityData): EnrichmentTransferActivity {
        return EnrichmentTransferActivity(
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            from = source.from,
            owner = source.owner,
            contract = source.contract,
            collection = source.getEnrichedCollection(data)?.fullId() ?: source.collection?.fullId(),
            tokenId = source.tokenId,
            itemId = source.itemId!!.fullId(),
            value = source.value,
            purchase = source.purchase,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo
        )
    }

    private fun convert(source: UnionOrderMatchSwap, data: EnrichmentActivityData): EnrichmentOrderMatchSwap {
        return EnrichmentOrderMatchSwap(
            orderId = source.orderId,
            source = source.source,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo,
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            left = convert(source.left, data),
            right = convert(source.right, data)
        )
    }

    private fun convert(source: UnionOrderMatchSell, data: EnrichmentActivityData): EnrichmentOrderMatchSell {
        return EnrichmentOrderMatchSell(
            orderId = source.orderId,
            source = source.source,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo,
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            nft = source.nft,
            payment = source.payment,
            buyer = source.buyer,
            seller = source.seller,
            buyerOrderHash = source.buyerOrderHash,
            sellerOrderHash = source.sellerOrderHash,
            price = source.price,
            priceUsd = source.priceUsd,
            amountUsd = source.amountUsd,
            type = convert(source.type),
            sellMarketplaceMarker = source.sellMarketplaceMarker,
            buyMarketplaceMarker = source.buyMarketplaceMarker,
            itemId = source.nft.type.itemId()!!.fullId(),
            contract = source.nft.type.contract(),
            collection = source.getEnrichedCollection(data)?.fullId(),
        )
    }

    private fun convert(source: UnionOrderBidActivity, data: EnrichmentActivityData): EnrichmentOrderBidActivity {
        return EnrichmentOrderBidActivity(
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            orderId = source.orderId,
            hash = source.hash,
            maker = source.maker,
            make = source.make,
            take = source.take,
            price = source.price,
            priceUsd = source.priceUsd,
            source = source.source,
            marketplaceMarker = source.marketplaceMarker,
            itemId = source.itemId()?.fullId(),
            collection = source.getEnrichedCollection(data)?.fullId(),
            contract = source.take.type.contract(),
        )
    }

    private fun convert(source: UnionOrderListActivity, data: EnrichmentActivityData): EnrichmentOrderListActivity {
        return EnrichmentOrderListActivity(
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            orderId = source.orderId,
            hash = source.hash,
            maker = source.maker,
            make = source.make,
            take = source.take,
            price = source.price,
            priceUsd = source.priceUsd,
            source = source.source,
            itemId = source.itemId()!!.fullId(),
            collection = source.getEnrichedCollection(data)?.fullId(),
            contract = source.make.type.contract(),
        )
    }

    private fun convert(
        source: UnionOrderCancelBidActivity,
        data: EnrichmentActivityData
    ): EnrichmentOrderCancelBidActivity {
        return EnrichmentOrderCancelBidActivity(
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            orderId = source.orderId,
            hash = source.hash,
            maker = source.maker,
            make = source.make,
            take = source.take,
            source = source.source,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo,
            itemId = source.itemId()?.fullId(),
            collection = source.getEnrichedCollection(data)?.fullId(),
            contract = source.take.contract(),
        )
    }

    private fun convert(
        source: UnionOrderCancelListActivity,
        data: EnrichmentActivityData
    ): EnrichmentOrderCancelListActivity {
        return EnrichmentOrderCancelListActivity(
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            orderId = source.orderId,
            hash = source.hash,
            maker = source.maker,
            make = source.make,
            take = source.take,
            source = source.source,
            transactionHash = source.transactionHash,
            blockchainInfo = source.blockchainInfo,
            itemId = source.itemId()!!.fullId(),
            collection = source.getEnrichedCollection(data)?.fullId(),
            contract = source.make.contract(),
        )
    }

    private fun convert(source: UnionAuctionOpenActivity, data: EnrichmentActivityData): EnrichmentAuctionOpenActivity {
        return EnrichmentAuctionOpenActivity(
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            auction = source.auction,
            transactionHash = source.transactionHash,
            itemId = source.itemId().fullId(),
            collection = source.getEnrichedCollection(data)?.fullId(),
        )
    }

    private fun convert(source: UnionAuctionBidActivity, data: EnrichmentActivityData): EnrichmentAuctionBidActivity {
        return EnrichmentAuctionBidActivity(
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            auction = source.auction,
            bid = source.bid,
            transactionHash = source.transactionHash,
            itemId = source.itemId().fullId(),
            collection = source.getEnrichedCollection(data)?.fullId(),
        )
    }

    private fun convert(
        source: UnionAuctionFinishActivity,
        data: EnrichmentActivityData
    ): EnrichmentAuctionFinishActivity {
        return EnrichmentAuctionFinishActivity(
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            auction = source.auction,
            transactionHash = source.transactionHash,
            itemId = source.itemId().fullId(),
            collection = source.getEnrichedCollection(data)?.fullId(),
        )
    }

    private fun convert(
        source: UnionAuctionCancelActivity,
        data: EnrichmentActivityData
    ): EnrichmentAuctionCancelActivity {
        return EnrichmentAuctionCancelActivity(
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            auction = source.auction,
            transactionHash = source.transactionHash,
            itemId = source.itemId().fullId(),
            collection = source.getEnrichedCollection(data)?.fullId(),
        )
    }

    private fun convert(
        source: UnionAuctionStartActivity,
        data: EnrichmentActivityData
    ): EnrichmentAuctionStartActivity {
        return EnrichmentAuctionStartActivity(
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            auction = source.auction,
            itemId = source.itemId().fullId(),
            collection = source.getEnrichedCollection(data)?.fullId(),
        )
    }

    private fun convert(source: UnionAuctionEndActivity, data: EnrichmentActivityData): EnrichmentAuctionEndActivity {
        return EnrichmentAuctionEndActivity(
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            auction = source.auction,
            itemId = source.itemId().fullId(),
            collection = source.getEnrichedCollection(data)?.fullId(),
        )
    }

    private fun convert(source: UnionL2DepositActivity, data: EnrichmentActivityData): EnrichmentL2DepositActivity {
        return EnrichmentL2DepositActivity(
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            user = source.user,
            status = source.status,
            itemId = source.itemId.fullId(),
            value = source.value,
            collection = source.getEnrichedCollection(data)?.fullId() ?: source.collection?.fullId(),
        )
    }

    private fun convert(
        source: UnionL2WithdrawalActivity,
        data: EnrichmentActivityData
    ): EnrichmentL2WithdrawalActivity {
        return EnrichmentL2WithdrawalActivity(
            activityId = source.id.value,
            blockchain = source.id.blockchain,
            date = source.date,
            lastUpdatedAt = source.lastUpdatedAt,
            user = source.user,
            status = source.status,
            itemId = source.itemId.fullId(),
            collection = source.getEnrichedCollection(data)?.fullId() ?: source.collection?.fullId(),
            value = source.value,
        )
    }

    private fun convert(source: UnionOrderMatchSell.Type): EnrichmentOrderMatchSell.Type {
        return when (source) {
            UnionOrderMatchSell.Type.SELL -> EnrichmentOrderMatchSell.Type.SELL
            UnionOrderMatchSell.Type.ACCEPT_BID -> EnrichmentOrderMatchSell.Type.ACCEPT_BID
        }
    }

    private fun convert(
        source: UnionOrderActivityMatchSideDto,
        data: EnrichmentActivityData
    ): EnrichmentOrderActivityMatchSide {
        return EnrichmentOrderActivityMatchSide(
            maker = source.maker,
            hash = source.hash,
            asset = source.asset
        )
    }

    private fun UnionActivity.getEnrichedCollection(data: EnrichmentActivityData): CollectionIdDto? {
        return data.customCollection ?: this.collectionId()
    }
}