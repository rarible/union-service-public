package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.L2DepositActivityDto
import com.rarible.protocol.union.dto.L2WithdrawalActivityDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderActivityMatchSideDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.OrderMatchSwapDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexDeposit
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfer
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexWithdrawal
import com.rarible.protocol.union.integration.immutablex.client.TradeSide
import org.slf4j.LoggerFactory
import scalether.domain.Address
import java.math.BigDecimal

object ImxActivityConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(
        activity: ImmutablexEvent,
        orders: Map<Long, ImmutablexOrder>,
        blockchain: BlockchainDto = BlockchainDto.IMMUTABLEX
    ): ActivityDto {
        try {
            return convertInternal(activity, orders, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Activity: {} \n{}", blockchain, e.message, activity)
            throw e
        }
    }

    private fun convertInternal(
        activity: ImmutablexEvent, orders: Map<Long, ImmutablexOrder>, blockchain: BlockchainDto
    ) = when (activity) {
        is ImmutablexMint -> MintActivityDto(
            id = activity.activityId,
            date = activity.timestamp,
            owner = UnionAddressConverter.convert(blockchain, activity.user),
            itemId = ItemIdDto(blockchain, activity.token.data.encodedItemId()),
            contract = ContractAddressConverter.convert(blockchain, activity.token.data.tokenAddress),
            tokenId = activity.token.data.encodedTokenId(),
            value = activity.token.data.quantity,
            transactionHash = activity.transactionId.toString(),
            blockchainInfo = null,
        )
        is ImmutablexTransfer -> {
            val from = UnionAddressConverter.convert(blockchain, activity.user)
            val to = UnionAddressConverter.convert(blockchain, activity.receiver)
            if (to.value == Address.ZERO().toString()) {
                BurnActivityDto(
                    id = activity.activityId,
                    date = activity.timestamp,
                    owner = from,
                    value = activity.token.data.quantity,
                    transactionHash = activity.transactionId.toString(),
                    itemId = ItemIdDto(blockchain, activity.encodedItemId()),
                    contract = ContractAddressConverter.convert(blockchain, activity.token.data.tokenAddress),
                    tokenId = activity.token.data.encodedTokenId(),
                )
            } else {
                TransferActivityDto(
                    id = activity.activityId,
                    date = activity.timestamp,
                    from = from,
                    owner = to,
                    itemId = ItemIdDto(blockchain, activity.encodedItemId()),
                    contract = ContractAddressConverter.convert(blockchain, activity.token.data.tokenAddress),
                    tokenId = activity.token.data.encodedTokenId(),
                    value = activity.token.data.quantity,
                    transactionHash = activity.transactionId.toString(),
                    blockchainInfo = null
                )
            }
        }
        is ImmutablexTrade -> {
            val makeAsset = convertAsset(activity.make, blockchain)
            val makeType = makeAsset.type.ext
            val takeAsset = convertAsset(activity.take, blockchain)
            val takeType = takeAsset.type.ext

            val maker = orders[activity.make.orderId]?.creator?.let { UnionAddressConverter.convert(blockchain, it) }
                ?: throw ImxDataException("$blockchain make Order ${activity.make.orderId} not found")
            val taker = orders[activity.take.orderId]?.creator?.let { UnionAddressConverter.convert(blockchain, it) }
                ?: throw ImxDataException("$blockchain take Order ${activity.take.orderId} not found")

            if (makeType.isNft && !takeType.isNft) {
                convertToMatch(activity, makeAsset, takeAsset, maker, taker)
            } else if (!makeType.isNft && takeType.isNft) {
                convertToMatch(activity, takeAsset, makeAsset, taker, maker)
            } else {
                OrderMatchSwapDto(
                    id = activity.activityId,
                    date = activity.timestamp,
                    source = OrderActivitySourceDto.RARIBLE,
                    transactionHash = activity.transactionId.toString(),
                    // TODO UNION remove in 1.19
                    blockchainInfo = null,
                    left = OrderActivityMatchSideDto(
                        maker = maker,
                        hash = null,
                        asset = makeAsset
                    ),
                    right = OrderActivityMatchSideDto(
                        maker = taker,
                        hash = null,
                        asset = takeAsset
                    ),
                    reverted = false,
                    lastUpdatedAt = activity.timestamp
                )
            }

        }

        // We don't need these activities ATM
        is ImmutablexDeposit -> L2DepositActivityDto(
            id = activity.activityId,
            date = activity.timestamp,
            user = UnionAddressConverter.convert(blockchain, activity.user),
            status = activity.status,
            itemId = ItemIdDto(blockchain, activity.encodedItemId()),
            value = activity.token.data.quantity
        )
        is ImmutablexWithdrawal -> L2WithdrawalActivityDto(
            id = activity.activityId,
            date = activity.timestamp,
            user = UnionAddressConverter.convert(blockchain, activity.sender),
            status = activity.status,
            itemId = ItemIdDto(blockchain, activity.encodedItemId()),
            value = activity.token.data.quantity
        )
    }

    private fun convertToMatch(
        activity: ImmutablexTrade,
        nft: AssetDto,
        payment: AssetDto,
        seller: UnionAddress,
        buyer: UnionAddress
    ): OrderMatchSellDto {
        // TODO IMMUTABLEX what if null?
        return OrderMatchSellDto(
            source = OrderActivitySourceDto.RARIBLE,
            transactionHash = activity.transactionId.toString(),
            blockchainInfo = null,
            id = activity.activityId,
            date = activity.timestamp,
            nft = nft,
            payment = payment,
            buyer = buyer,
            seller = seller,
            buyerOrderHash = activity.take.orderId.toString(),
            sellerOrderHash = activity.make.orderId.toString(),
            price = BigDecimal.ZERO,
            priceUsd = null,
            amountUsd = null,
            type = OrderMatchSellDto.Type.SELL
        )
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
                    asset.encodedTokenId()
                ),
                value = asset.sold
            )
            else -> throw IllegalStateException("Unsupported token type: ${asset.tokenType}")
        }

}