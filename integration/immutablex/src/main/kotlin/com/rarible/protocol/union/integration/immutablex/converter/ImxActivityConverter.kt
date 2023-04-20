package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionBurnActivity
import com.rarible.protocol.union.core.model.UnionL2DepositActivity
import com.rarible.protocol.union.core.model.UnionL2WithdrawalActivity
import com.rarible.protocol.union.core.model.UnionMintActivity
import com.rarible.protocol.union.core.model.UnionOrderActivityMatchSideDto
import com.rarible.protocol.union.core.model.UnionOrderMatchSell
import com.rarible.protocol.union.core.model.UnionOrderMatchSwap
import com.rarible.protocol.union.core.model.UnionTransferActivity
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexDeposit
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfer
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexWithdrawal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ImxActivityConverter(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(
        activity: ImmutablexEvent,
        orders: Map<Long, ImmutablexOrder>,
        blockchain: BlockchainDto
    ): UnionActivity {
        try {
            return convertInternal(activity, orders, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Activity: {} \n{}", blockchain, e.message, activity)
            throw e
        }
    }

    private suspend fun convertInternal(
        activity: ImmutablexEvent,
        orders: Map<Long, ImmutablexOrder>,
        blockchain: BlockchainDto
    ) = when (activity) {
        is ImmutablexMint -> UnionMintActivity(
            id = activity.activityId,
            date = activity.timestamp,
            owner = UnionAddressConverter.convert(blockchain, activity.user),
            itemId = ItemIdDto(blockchain, activity.token.data.encodedItemId()),
            contract = ContractAddressConverter.convert(blockchain, activity.token.data.tokenAddress),
            collection = CollectionIdDto(blockchain, activity.token.data.tokenAddress),
            tokenId = activity.token.data.encodedTokenId(),
            value = activity.token.data.quantity,
            transactionHash = activity.transactionId.toString(),
            blockchainInfo = null,
        )
        is ImmutablexTransfer -> {
            val from = UnionAddressConverter.convert(blockchain, activity.user)
            val to = UnionAddressConverter.convert(blockchain, activity.receiver)
            if (to.value == Address.ZERO().toString()) {
                UnionBurnActivity(
                    id = activity.activityId,
                    date = activity.timestamp,
                    owner = from,
                    value = activity.token.data.quantity,
                    transactionHash = activity.transactionId.toString(),
                    itemId = ItemIdDto(blockchain, activity.encodedItemId()),
                    contract = ContractAddressConverter.convert(blockchain, activity.token.data.tokenAddress),
                    collection = CollectionIdDto(blockchain, activity.token.data.tokenAddress),
                    tokenId = activity.token.data.encodedTokenId(),
                )
            } else {
                UnionTransferActivity(
                    id = activity.activityId,
                    date = activity.timestamp,
                    from = from,
                    owner = to,
                    itemId = ItemIdDto(blockchain, activity.encodedItemId()),
                    contract = ContractAddressConverter.convert(blockchain, activity.token.data.tokenAddress),
                    collection = CollectionIdDto(blockchain, activity.token.data.tokenAddress),
                    tokenId = activity.token.data.encodedTokenId(),
                    value = activity.token.data.quantity,
                    transactionHash = activity.transactionId.toString(),
                    blockchainInfo = null
                )
            }
        }
        is ImmutablexTrade -> {
            val makeOrder = orders[activity.make.orderId]
                ?: throw ImxDataException("$blockchain make Order ${activity.make.orderId} not found")

            val takeOrder = orders[activity.take.orderId]
                ?: throw ImxDataException("$blockchain take Order ${activity.take.orderId} not found")

            val makeAsset = ImxOrderConverter.toAsset(makeOrder, makeOrder.sell, blockchain)
            val makeType = makeAsset.type
            val takeAsset = ImxOrderConverter.toAsset(takeOrder, takeOrder.sell, blockchain)
            val takeType = takeAsset.type

            val maker = UnionAddressConverter.convert(blockchain, makeOrder.creator)
            val taker = UnionAddressConverter.convert(blockchain, takeOrder.creator)

            if (makeType.isNft() && !takeType.isNft()) {
                convertToMatch(activity, makeAsset, takeAsset, maker, taker)
            } else if (!makeType.isNft() && takeType.isNft()) {
                convertToMatch(activity, takeAsset, makeAsset, taker, maker)
            } else {
                UnionOrderMatchSwap(
                    id = activity.activityId,
                    date = activity.timestamp,
                    source = OrderActivitySourceDto.RARIBLE,
                    transactionHash = activity.transactionId.toString(),
                    left = UnionOrderActivityMatchSideDto(
                        maker = maker,
                        hash = activity.make.orderId.toString(),
                        asset = makeAsset
                    ),
                    right = UnionOrderActivityMatchSideDto(
                        maker = taker,
                        hash = activity.take.orderId.toString(),
                        asset = takeAsset
                    ),
                    reverted = false,
                    lastUpdatedAt = activity.timestamp,
                    blockchainInfo = null,
                )
            }

        }

        // We don't need these activities ATM
        is ImmutablexDeposit -> UnionL2DepositActivity(
            id = activity.activityId,
            date = activity.timestamp,
            user = UnionAddressConverter.convert(blockchain, activity.user),
            status = activity.status,
            itemId = ItemIdDto(blockchain, activity.encodedItemId()),
            collection = CollectionIdDto(blockchain, activity.token.data.tokenAddress),
            value = activity.token.data.quantity
        )

        is ImmutablexWithdrawal -> UnionL2WithdrawalActivity(
            id = activity.activityId,
            date = activity.timestamp,
            user = UnionAddressConverter.convert(blockchain, activity.sender),
            status = activity.status,
            itemId = ItemIdDto(blockchain, activity.encodedItemId()),
            collection = CollectionIdDto(blockchain, activity.token.data.tokenAddress),
            value = activity.token.data.quantity
        )
    }

    private suspend fun convertToMatch(
        activity: ImmutablexTrade,
        nft: UnionAsset,
        payment: UnionAsset,
        seller: UnionAddress,
        buyer: UnionAddress
    ): UnionOrderMatchSell {
        val priceUsd = currencyService.toUsd(
            BlockchainDto.ETHEREUM, payment.type, payment.value, activity.timestamp
        )

        return UnionOrderMatchSell(
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
            price = payment.value,
            priceUsd = priceUsd,
            amountUsd = null,
            type = UnionOrderMatchSell.Type.SELL
        )
    }
}
