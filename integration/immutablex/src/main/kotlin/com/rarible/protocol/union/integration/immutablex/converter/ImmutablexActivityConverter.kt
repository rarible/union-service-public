package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTransfer
import com.rarible.protocol.union.integration.immutablex.dto.TradeSide
import org.slf4j.LoggerFactory
import java.math.BigDecimal

object ImmutablexActivityConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(
        activity: ImmutablexEvent, orders: Map<Long, OrderDto>, blockchain: BlockchainDto = BlockchainDto.IMMUTABLEX
    ): ActivityDto {
        try {
            return convertInternal(activity, orders, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Activity: {} \n{}", blockchain, e.message, activity)
            throw e
        }
    }

    private fun convertInternal(
        activity: ImmutablexEvent, orders: Map<Long, OrderDto>, blockchain: BlockchainDto
    ) = when (activity) {
        is ImmutablexMint -> MintActivityDto(
            id = activity.activityId,
            date = activity.timestamp,
            owner = UnionAddressConverter.convert(blockchain, activity.user),
            itemId = ItemIdDto(blockchain, activity.token.data.itemId()),
            value = activity.token.data.quantity,
            transactionHash = activity.transactionId.toString(),
            blockchainInfo = null,
        )
        is ImmutablexTransfer -> TransferActivityDto(
            id = activity.activityId,
            date = activity.timestamp,
            from = UnionAddressConverter.convert(blockchain, activity.user),
            owner = UnionAddressConverter.convert(blockchain, activity.receiver),
            itemId = ItemIdDto(blockchain, activity.token.data.itemId()),
            value = activity.token.data.quantity,
            transactionHash = activity.transactionId.toString(),
            blockchainInfo = null
        )
        is ImmutablexTrade -> {
            // TODO IMMUTABLEX what if null?
            val makeOrder = orders[activity.make.orderId]
                ?: throw NullPointerException("$blockchain make Order ${activity.make.orderId} not found")
            val takeOrder = orders[activity.take.orderId]
                ?: throw NullPointerException("$blockchain take Order ${activity.take.orderId} not found")

            OrderMatchSellDto(
                source = OrderActivitySourceDto.RARIBLE,
                transactionHash = activity.transactionId.toString(),
                blockchainInfo = null,
                id = activity.activityId,
                date = activity.timestamp,
                nft = convertAsset(activity.make, blockchain),
                payment = convertAsset(activity.take, blockchain),
                buyer = takeOrder.maker,
                seller = makeOrder.maker,
                buyerOrderHash = activity.take.orderId.toString(),
                sellerOrderHash = activity.make.orderId.toString(),
                price = BigDecimal.ZERO,
                priceUsd = null,
                amountUsd = null,
                type = OrderMatchSellDto.Type.SELL,
            )
        }
        else -> throw IllegalStateException("Unsupported activity type ${activity::class.simpleName}")
    }

    private fun convertAsset(asset: TradeSide, blockchain: BlockchainDto) =
        when (asset.tokenType) {
            "ETH" -> AssetDto(
                type = EthEthereumAssetTypeDto(blockchain),
                value = asset.sold.setScale(18)
            )
            "ERC20" -> AssetDto(
                type = EthErc20AssetTypeDto(ContractAddressConverter.convert(blockchain, asset.tokenAddress!!)),
                value = asset.sold.setScale(18)
            )
            "ERC721" -> AssetDto(
                type = EthErc721AssetTypeDto(
                    ContractAddressConverter.convert(blockchain, asset.tokenAddress!!),
                    // TODO could it be UUID instead of BigInteger?
                    asset.tokenId!!.toBigInteger()
                ),
                value = asset.sold
            )
            else -> throw IllegalStateException("Unsupported token type: ${asset.tokenType}")
        }

}
