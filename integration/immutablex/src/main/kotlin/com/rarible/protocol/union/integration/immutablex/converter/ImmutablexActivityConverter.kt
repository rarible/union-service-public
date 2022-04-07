package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTransfer
import com.rarible.protocol.union.integration.immutablex.dto.TradeSide
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexOrderService
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class ImmutablexActivityConverter(
    private val orderService: ImmutablexOrderService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(activity: ImmutablexEvent, blockchain: BlockchainDto = BlockchainDto.IMMUTABLEX): ActivityDto {
        try {
            return convertInternal(activity, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Activity: {} \n{}", blockchain, e.message, activity)
            throw e
        }
    }

    private fun convertInternal(activity: ImmutablexEvent, blockchain: BlockchainDto) = when (activity) {
        is ImmutablexMint -> MintActivityDto(
            id = activity.activityId,
            date = activity.timestamp,
            owner = unionAddress(activity.user, blockchain),
            contract = contractAddress(activity.token.data.tokenAddress!!, blockchain),
            tokenId = activity.token.data.tokenId!!.toBigInteger(),
            value = activity.token.data.quantity?.toBigInteger() ?: BigInteger.ONE,
            transactionHash = activity.transactionId.toString(),
            blockchainInfo = null,
        )
        is ImmutablexTransfer -> TransferActivityDto(
            id = activity.activityId,
            date = activity.timestamp,
            from = unionAddress(activity.user, blockchain),
            owner = unionAddress(activity.receiver, blockchain),
            contract = contractAddress(activity.token.data.tokenAddress!!, blockchain),
            tokenId = activity.token.data.tokenId!!.toBigInteger(),
            value = activity.token.data.quantity?.toBigInteger() ?: BigInteger.ONE,
            transactionHash = activity.transactionId.toString(),
            blockchainInfo = null,
        )
        is ImmutablexTrade -> {
            val (makeOrder, takeOrder) = runBlocking(Dispatchers.IO) {
                orderService.getOrderById("${activity.make.orderId}") to orderService.getOrderById("${activity.take.orderId}")
            }

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
                type = EthErc20AssetTypeDto(contractAddress(asset.tokenAddress!!, blockchain)),
                value = asset.sold.setScale(18)
            )
            "ERC721" -> AssetDto(
                type = EthErc721AssetTypeDto(contractAddress(asset.tokenAddress!!, blockchain), asset.tokenId!!.toBigInteger()),
                value = asset.sold
            )
            else -> throw IllegalStateException("Unsupported token type: ${asset.tokenType}")
        }

    private fun unionAddress(value: String, blockchain: BlockchainDto) = UnionAddress(blockchain.group(), value)

    private fun contractAddress(value: String, blockchain: BlockchainDto) = ContractAddress(blockchain, value)
}
