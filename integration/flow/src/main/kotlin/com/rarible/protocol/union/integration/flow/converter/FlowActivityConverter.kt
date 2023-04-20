package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowBurnDto
import com.rarible.protocol.dto.FlowMintDto
import com.rarible.protocol.dto.FlowNftOrderActivityBidDto
import com.rarible.protocol.dto.FlowNftOrderActivityCancelBidDto
import com.rarible.protocol.dto.FlowNftOrderActivityCancelListDto
import com.rarible.protocol.dto.FlowNftOrderActivityListDto
import com.rarible.protocol.dto.FlowNftOrderActivitySellDto
import com.rarible.protocol.dto.FlowOrderActivityMatchSideDto
import com.rarible.protocol.dto.FlowTransferDto
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionActivityDto
import com.rarible.protocol.union.core.model.UnionAssetDto
import com.rarible.protocol.union.core.model.UnionBurnActivityDto
import com.rarible.protocol.union.core.model.UnionMintActivityDto
import com.rarible.protocol.union.core.model.UnionOrderActivityMatchSideDto
import com.rarible.protocol.union.core.model.UnionOrderBidActivityDto
import com.rarible.protocol.union.core.model.UnionOrderCancelBidActivityDto
import com.rarible.protocol.union.core.model.UnionOrderCancelListActivityDto
import com.rarible.protocol.union.core.model.UnionOrderListActivityDto
import com.rarible.protocol.union.core.model.UnionOrderMatchSellDto
import com.rarible.protocol.union.core.model.UnionOrderMatchSwapDto
import com.rarible.protocol.union.core.model.UnionTransferActivityDto
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class FlowActivityConverter(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(source: FlowActivityDto): UnionActivityDto {
        try {
            return convertInternal(source)
        } catch (e: Exception) {
            logger.error("Failed to convert Flow Activity: {} \n{}", e.message, source)
            throw e
        }
    }

    private suspend fun convertInternal(source: FlowActivityDto): UnionActivityDto {
        val blockchain = BlockchainDto.FLOW
        val activityId = ActivityIdDto(blockchain, source.id)
        return when (source) {
            is FlowNftOrderActivitySellDto -> {
                val leftSide = source.left
                val rightSide = source.right
                val leftType = FlowConverter.convert(leftSide.asset, blockchain).type
                val rightTypeExt = FlowConverter.convert(rightSide.asset, blockchain).type
                if (leftType.isNft() && rightTypeExt.isCurrency()) {
                    activityToSell(
                        activityId = activityId,
                        source = source,
                        blockchain = blockchain,
                        nft = leftSide,
                        payment = rightSide,
                        type = UnionOrderMatchSellDto.Type.SELL
                    )
                } else if (leftType.isCurrency() && rightTypeExt.isNft()) {
                    activityToSell(
                        activityId = activityId,
                        source = source,
                        blockchain = blockchain,
                        nft = rightSide,
                        payment = leftSide,
                        type = UnionOrderMatchSellDto.Type.ACCEPT_BID
                    )
                } else {
                    activityToSwap(
                        activityId = activityId,
                        source = source,
                        blockchain = blockchain
                    )
                }
            }
            is FlowNftOrderActivityListDto -> {
                val payment = FlowConverter.convert(source.take, blockchain)
                val priceUsd = currencyService
                    .toUsd(blockchain, payment.type, source.price) ?: BigDecimal.ZERO

                UnionOrderListActivityDto(
                    id = activityId,
                    date = source.date,
                    price = source.price,
                    // TODO FLOW here should be price from FLOW, we don't want to calculate it here
                    priceUsd = priceUsd,
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(blockchain, source.maker),
                    make = FlowConverter.convert(source.make, blockchain),
                    take = payment,
                    reverted = source.reverted,
                    lastUpdatedAt = source.updatedAt
                )
            }
            is FlowNftOrderActivityCancelListDto -> {
                UnionOrderCancelListActivityDto(
                    id = activityId,
                    date = source.date,
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(blockchain, source.maker),
                    make = FlowConverter.convertToType(source.make, blockchain),
                    take = FlowConverter.convertToType(source.take, blockchain),
                    transactionHash = source.transactionHash ?: "",
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash ?: "",
                        blockHash = source.blockHash ?: "",
                        blockNumber = source.blockNumber ?: 0,
                        logIndex = source.logIndex ?: 0
                    ),
                    reverted = source.reverted,
                    lastUpdatedAt = source.updatedAt
                )
            }
            is FlowMintDto -> {
                UnionMintActivityDto(
                    id = activityId,
                    date = source.date,
                    owner = UnionAddressConverter.convert(blockchain, source.owner),
                    contract = ContractAddressConverter.convert(blockchain, source.contract),
                    collection = CollectionIdDto(blockchain, source.contract),
                    tokenId = source.tokenId, // TODO remove later
                    itemId = ItemIdDto(blockchain, source.contract, source.tokenId),
                    value = source.value,
                    transactionHash = source.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash,
                        blockHash = source.blockHash,
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    ),
                    reverted = source.reverted,
                    lastUpdatedAt = source.updatedAt
                )
            }
            is FlowBurnDto -> {
                UnionBurnActivityDto(
                    id = activityId,
                    date = source.date,
                    owner = UnionAddressConverter.convert(blockchain, source.owner),
                    contract = ContractAddressConverter.convert(blockchain, source.contract),
                    collection = CollectionIdDto(blockchain, source.contract),
                    tokenId = source.tokenId, // TODO remove later
                    itemId = ItemIdDto(blockchain, source.contract, source.tokenId),
                    value = source.value,
                    transactionHash = source.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash,
                        blockHash = source.blockHash,
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    ),
                    reverted = source.reverted,
                    lastUpdatedAt = source.updatedAt
                )
            }
            is FlowTransferDto -> {
                UnionTransferActivityDto(
                    id = activityId,
                    date = source.date,
                    from = UnionAddressConverter.convert(blockchain, source.from),
                    owner = UnionAddressConverter.convert(blockchain, source.owner),
                    contract = ContractAddressConverter.convert(blockchain, source.contract),
                    collection = CollectionIdDto(blockchain, source.contract),
                    tokenId = source.tokenId, // TODO remove later
                    itemId = ItemIdDto(blockchain, source.contract, source.tokenId),
                    value = source.value,
                    transactionHash = source.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash,
                        blockHash = source.blockHash,
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    ),
                    reverted = source.reverted,
                    purchase = source.purchased,
                    lastUpdatedAt = source.updatedAt
                )
            }
            is FlowNftOrderActivityBidDto -> {
                val payment = FlowConverter.convert(source.make, blockchain)
                val priceUsd = currencyService
                    .toUsd(blockchain, payment.type, source.price) ?: BigDecimal.ZERO

                UnionOrderBidActivityDto(
                    id = activityId,
                    date = source.date,
                    price = source.price,
                    // TODO FLOW here should be price from FLOW, we don't want to calculate it here
                    priceUsd = priceUsd,
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(blockchain, source.maker),
                    make = payment,
                    take = FlowConverter.convert(source.take, blockchain),
                    reverted = source.reverted,
                    lastUpdatedAt = source.updatedAt
                )
            }
            is FlowNftOrderActivityCancelBidDto -> {
                UnionOrderCancelBidActivityDto(
                    id = activityId,
                    date = source.date,
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(blockchain, source.maker),
                    make = FlowConverter.convertToType(source.make, blockchain),
                    take = FlowConverter.convertToType(source.take, blockchain),
                    transactionHash = source.transactionHash ?: "",
                    reverted = source.reverted,
                    lastUpdatedAt = source.updatedAt
                )
            }
            else -> throw IllegalStateException("Unsupported flow activity! $source")
        }
    }

    private suspend fun activityToSell(
        source: FlowNftOrderActivitySellDto,
        blockchain: BlockchainDto,
        nft: FlowOrderActivityMatchSideDto,
        payment: FlowOrderActivityMatchSideDto,
        type: UnionOrderMatchSellDto.Type,
        activityId: ActivityIdDto
    ): UnionOrderMatchSellDto {
        val unionPayment = FlowConverter.convert(payment.asset, blockchain)
        val unionNft = FlowConverter.convert(nft.asset, blockchain)

        val priceUsd = currencyService
            .toUsd(blockchain, unionPayment.type, source.price) ?: BigDecimal.ZERO

        return UnionOrderMatchSellDto(
            id = activityId,
            date = source.date,
            source = OrderActivitySourceDto.RARIBLE,
            nft = unionNft,
            payment = unionPayment,
            seller = UnionAddressConverter.convert(blockchain, nft.maker),
            buyer = UnionAddressConverter.convert(blockchain, payment.maker),
            price = source.price,
            priceUsd = priceUsd,
            amountUsd = amountUsd(priceUsd, unionNft),
            type = type,
            // TODO FLOW there is no order info in flow for sides
            sellerOrderHash = null,
            buyerOrderHash = null,
            transactionHash = source.transactionHash,
            // TODO UNION remove in 1.19
            blockchainInfo = ActivityBlockchainInfoDto(
                transactionHash = source.transactionHash,
                blockHash = source.blockHash,
                blockNumber = source.blockNumber,
                logIndex = source.logIndex
            ),
            reverted = source.reverted,
            lastUpdatedAt = source.updatedAt
        )
    }

    private fun activityToSwap(
        source: FlowNftOrderActivitySellDto,
        blockchain: BlockchainDto,
        activityId: ActivityIdDto
    ) = UnionOrderMatchSwapDto(
        id = activityId,
        date = source.date,
        source = OrderActivitySourceDto.RARIBLE,
        transactionHash = source.transactionHash,
        // TODO UNION remove in 1.19
        blockchainInfo = ActivityBlockchainInfoDto(
            transactionHash = source.transactionHash,
            blockHash = source.blockHash,
            blockNumber = source.blockNumber,
            logIndex = source.logIndex
        ),
        left = convert(source.left, blockchain),
        right = convert(source.right, blockchain),
        reverted = source.reverted,
        lastUpdatedAt = source.updatedAt
    )

    private fun convert(
        source: FlowOrderActivityMatchSideDto,
        blockchain: BlockchainDto
    ): UnionOrderActivityMatchSideDto {
        return UnionOrderActivityMatchSideDto(
            maker = UnionAddressConverter.convert(blockchain, source.maker),
            hash = null,
            asset = FlowConverter.convert(source.asset, blockchain),
        )
    }

    private fun amountUsd(price: BigDecimal, asset: UnionAssetDto) = price.multiply(asset.value)

    suspend fun convert(source: FlowActivitiesDto): List<UnionActivityDto> {
        return source.items.map { convert(it) }
    }

    fun convert(source: SyncSortDto?): String =
        when (source) {
            SyncSortDto.DB_UPDATE_DESC -> "LATEST_FIRST"
            else -> "EARLIEST_FIRST"
        }

    fun convert(source: SyncTypeDto?): List<String> =
        when (source) {
            SyncTypeDto.ORDER -> ORDER_LIST
            SyncTypeDto.NFT -> NFT_LIST
            SyncTypeDto.AUCTION -> emptyList()
            else -> ALL_LIST
        }

    companion object {
        val ORDER_LIST = listOf("SELL", "LIST", "CANCEL_LIST", "BID", "CANCEL_BID")
        val NFT_LIST = listOf("TRANSFER", "MINT", "BURN")
        val ALL_LIST = ORDER_LIST + NFT_LIST
    }
}
