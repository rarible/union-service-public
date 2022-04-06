package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.integration.immutablex.dto.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.BigInteger

class ImmutablexActivityConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(activity: ImmutablexEvent): ActivityDto {
        try {
            return convertInternal(activity)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Activity: {} \n{}", BlockchainDto.IMMUTABLEX, e.message, activity)
            throw e
        }
    }

    private fun convertInternal(activity: ImmutablexEvent) = when (activity) {
        is ImmutablexMint -> MintActivityDto(
            id = activity.activityId,
            date = activity.timestamp,
            owner = unionAddress(activity.user),
            contract = contractAddress(activity.token.data.tokenAddress!!),
            tokenId = activity.token.data.tokenId!!.toBigInteger(),
            value = activity.token.data.quantity?.toBigInteger() ?: BigInteger.ONE,
            transactionHash = activity.transactionId.toString(),
            blockchainInfo = null,
        )
        is ImmutablexTransfer -> TransferActivityDto(
            id = activity.activityId,
            date = activity.timestamp,
            from = unionAddress(activity.user),
            owner = unionAddress(activity.receiver),
            contract = contractAddress(activity.token.data.tokenAddress!!),
            tokenId = activity.token.data.tokenId!!.toBigInteger(),
            value = activity.token.data.quantity?.toBigInteger() ?: BigInteger.ONE,
            transactionHash = activity.transactionId.toString(),
            blockchainInfo = null,
        )
        is ImmutablexTrade -> OrderMatchSellDto(
            source = OrderActivitySourceDto.RARIBLE,
            transactionHash = activity.transactionId.toString(),
            blockchainInfo = null,
            id = activity.activityId,
            date = activity.timestamp,
            nft = convertAsset(activity.make),
            payment = convertAsset(activity.take),
            buyer = unionAddress(""),
            seller = unionAddress(""),
            buyerOrderHash = activity.take.orderId.toString(),
            sellerOrderHash = activity.make.orderId.toString(),
            price = BigDecimal.ZERO,
            priceUsd = null,
            amountUsd = null,
            type = OrderMatchSellDto.Type.SELL,
        )
        else -> throw IllegalStateException("Unsupported activity type")
    }

    private fun convertAsset(asset: TradeSide) =
        when (asset.tokenType) {
            "ETH" -> AssetDto(
                type = EthEthereumAssetTypeDto(BlockchainDto.IMMUTABLEX),
                value = asset.sold.setScale(18)
            )
            "ERC20" -> AssetDto(
                type = EthErc20AssetTypeDto(contractAddress(asset.tokenAddress!!)),
                value = asset.sold.setScale(18)
            )
            "ERC721" -> AssetDto(
                type = EthErc721AssetTypeDto(contractAddress(asset.tokenAddress!!), asset.tokenId!!.toBigInteger()),
                value = asset.sold
            )
            else -> throw IllegalStateException("Unsupported token type: ${asset.tokenType}")
        }

    private fun unionAddress(value: String) = UnionAddress(BlockchainGroupDto.IMMUTABLEX, value)

    private fun contractAddress(value: String) = ContractAddress(BlockchainDto.IMMUTABLEX, value)
}
