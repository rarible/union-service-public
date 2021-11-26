package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.tezos.dto.BurnDto
import com.rarible.protocol.tezos.dto.MintDto
import com.rarible.protocol.tezos.dto.NftActTypeDto
import com.rarible.protocol.tezos.dto.NftActivityFilterAllTypeDto
import com.rarible.protocol.tezos.dto.NftActivityFilterUserTypeDto
import com.rarible.protocol.tezos.dto.OrderActTypeDto
import com.rarible.protocol.tezos.dto.OrderActivityBidDto
import com.rarible.protocol.tezos.dto.OrderActivityCancelBidDto
import com.rarible.protocol.tezos.dto.OrderActivityCancelListDto
import com.rarible.protocol.tezos.dto.OrderActivityFilterAllTypeDto
import com.rarible.protocol.tezos.dto.OrderActivityFilterUserTypeDto
import com.rarible.protocol.tezos.dto.OrderActivityListDto
import com.rarible.protocol.tezos.dto.OrderActivityMatchDto
import com.rarible.protocol.tezos.dto.OrderActivityMatchTypeDto
import com.rarible.protocol.tezos.dto.OrderActivitySideMatchDto
import com.rarible.protocol.tezos.dto.TransferDto
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderActivityMatchSideDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelListActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.OrderMatchSwapDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.ext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TezosActivityConverter(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(source: OrderActTypeDto, blockchain: BlockchainDto): ActivityDto {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Order Activity: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    suspend fun convert(source: NftActTypeDto, blockchain: BlockchainDto): ActivityDto {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} NFT Activity: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    private suspend fun convertInternal(actType: OrderActTypeDto, blockchain: BlockchainDto): ActivityDto {
        val activityId = ActivityIdDto(blockchain, actType.id)
        val date = actType.date
        val source = convertSource(actType.source)

        return when (val activity = actType.type) {
            is OrderActivityMatchDto -> {
                val type = convertType(activity.type)
                val leftSide = activity.left
                val rightSide = activity.right
                val leftTypeExt = TezosConverter.convert(leftSide.asset.assetType, blockchain).ext
                val rightTypeExt = TezosConverter.convert(rightSide.asset.assetType, blockchain).ext
                if (leftTypeExt.isNft && rightTypeExt.isCurrency) {
                    activityToSell(
                        activityId = activityId,
                        activity = activity,
                        blockchain = blockchain,
                        nft = leftSide,
                        payment = rightSide,
                        type = type,
                        date = date,
                        source = source
                    )
                } else if (leftTypeExt.isCurrency && rightTypeExt.isNft) {
                    activityToSell(
                        activityId = activityId,
                        activity = activity,
                        blockchain = blockchain,
                        nft = rightSide,
                        payment = leftSide,
                        type = type,
                        date = date,
                        source = source
                    )
                } else {
                    activityToSwap(
                        activityId = activityId,
                        activity = activity,
                        blockchain = blockchain,
                        date = date,
                        source = source
                    )
                }
            }
            is OrderActivityBidDto -> {
                val payment = TezosConverter.convert(activity.make, blockchain)
                val nft = TezosConverter.convert(activity.take, blockchain)
                OrderBidActivityDto(
                    id = activityId,
                    date = date,
                    price = activity.price,
                    priceUsd = currencyService.toUsd(blockchain, payment.type, activity.price, date),
                    source = convertSource(actType.source),
                    hash = activity.hash,
                    maker = UnionAddressConverter.convert(blockchain, activity.maker),
                    make = payment,
                    take = nft
                )
            }
            is OrderActivityListDto -> {
                val payment = TezosConverter.convert(activity.take, blockchain)
                val nft = TezosConverter.convert(activity.make, blockchain)
                OrderListActivityDto(
                    id = activityId,
                    date = date,
                    price = activity.price,
                    priceUsd = currencyService.toUsd(blockchain, payment.type, activity.price, date),
                    source = convertSource(actType.source),
                    hash = activity.hash,
                    maker = UnionAddressConverter.convert(blockchain, activity.maker),
                    make = nft,
                    take = payment
                )
            }
            is OrderActivityCancelBidDto -> {
                OrderCancelBidActivityDto(
                    id = activityId,
                    date = date,
                    source = source,
                    hash = activity.hash,
                    maker = UnionAddressConverter.convert(blockchain, activity.maker),
                    make = TezosConverter.convert(activity.make, blockchain),
                    take = TezosConverter.convert(activity.take, blockchain),
                    transactionHash = activity.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = activity.transactionHash,
                        blockHash = activity.blockHash,
                        blockNumber = activity.blockNumber.toLong(),
                        logIndex = activity.logIndex
                    )
                )
            }
            is OrderActivityCancelListDto -> {
                OrderCancelListActivityDto(
                    id = activityId,
                    date = date,
                    source = source,
                    hash = activity.hash,
                    maker = UnionAddressConverter.convert(blockchain, activity.maker),
                    make = TezosConverter.convert(activity.make, blockchain),
                    take = TezosConverter.convert(activity.take, blockchain),
                    transactionHash = activity.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = activity.transactionHash,
                        blockHash = activity.blockHash,
                        blockNumber = activity.blockNumber.toLong(),
                        logIndex = activity.logIndex
                    )
                )
            }

        }
    }

    private suspend fun convertInternal(actType: NftActTypeDto, blockchain: BlockchainDto): ActivityDto {
        val activityId = ActivityIdDto(blockchain, actType.id)
        val date = actType.date
        return when (val activity = actType.type) {
            is MintDto -> {
                MintActivityDto(
                    id = activityId,
                    date = date,
                    owner = UnionAddressConverter.convert(blockchain, activity.owner),
                    contract = ContractAddressConverter.convert(blockchain, activity.contract),
                    tokenId = activity.tokenId,
                    // Tezos send it as BigDecimal, but in fact, that's BigInteger
                    value = activity.value.toBigInteger(),
                    transactionHash = activity.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = activity.transactionHash,
                        blockHash = activity.blockHash,
                        blockNumber = activity.blockNumber.toLong(),
                        logIndex = /*source.elt.logIndex*/ 0 // TODO UNION we're planning to remove it
                    )
                )
            }
            is BurnDto -> {
                BurnActivityDto(
                    id = activityId,
                    date = date,
                    owner = UnionAddressConverter.convert(blockchain, activity.owner),
                    contract = ContractAddressConverter.convert(blockchain, activity.contract),
                    tokenId = activity.tokenId,
                    // Tezos send it as BigDecimal, but in fact, that's BigInteger
                    value = activity.value.toBigInteger(),
                    transactionHash = activity.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = activity.transactionHash,
                        blockHash = activity.blockHash,
                        blockNumber = activity.blockNumber.toLong(),
                        logIndex = /*source.elt.logIndex*/ 0 // TODO UNION we're planning to remove it
                    )
                )
            }
            is TransferDto -> {
                TransferActivityDto(
                    id = activityId,
                    date = date,
                    from = UnionAddressConverter.convert(blockchain, activity.from),
                    owner = UnionAddressConverter.convert(blockchain, activity.elt.owner),
                    contract = ContractAddressConverter.convert(blockchain, activity.elt.contract),
                    tokenId = activity.elt.tokenId,
                    // Tezos send it as BigDecimal, but in fact, that's BigInteger
                    value = activity.elt.value.toBigInteger(),
                    transactionHash = activity.elt.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = activity.elt.transactionHash,
                        blockHash = activity.elt.blockHash,
                        blockNumber = activity.elt.blockNumber.toLong(),
                        logIndex = /*source.elt.logIndex*/ 0 // TODO UNION we're planning to remove it
                    )
                )
            }
        }
    }

    fun asUserOrderActivityType(source: UserActivityTypeDto): OrderActivityFilterUserTypeDto? {
        return when (source) {
            UserActivityTypeDto.MAKE_BID -> OrderActivityFilterUserTypeDto.MAKE_BID
            UserActivityTypeDto.GET_BID -> OrderActivityFilterUserTypeDto.GET_BID
            UserActivityTypeDto.BUY -> OrderActivityFilterUserTypeDto.BUY
            UserActivityTypeDto.LIST -> OrderActivityFilterUserTypeDto.LIST
            UserActivityTypeDto.SELL -> OrderActivityFilterUserTypeDto.SELL
            else -> null
        }
    }

    fun asUserNftActivityType(source: UserActivityTypeDto): NftActivityFilterUserTypeDto? {
        return when (source) {
            UserActivityTypeDto.TRANSFER_FROM -> NftActivityFilterUserTypeDto.TRANSFER_FROM
            UserActivityTypeDto.TRANSFER_TO -> NftActivityFilterUserTypeDto.TRANSFER_TO
            UserActivityTypeDto.MINT -> NftActivityFilterUserTypeDto.MINT
            UserActivityTypeDto.BURN -> NftActivityFilterUserTypeDto.BURN
            else -> null
        }
    }

    fun asOrderActivityType(source: com.rarible.protocol.union.dto.ActivityTypeDto): OrderActivityFilterAllTypeDto? {
        return when (source) {
            com.rarible.protocol.union.dto.ActivityTypeDto.BID -> OrderActivityFilterAllTypeDto.BID
            com.rarible.protocol.union.dto.ActivityTypeDto.LIST -> OrderActivityFilterAllTypeDto.LIST
            com.rarible.protocol.union.dto.ActivityTypeDto.SELL -> OrderActivityFilterAllTypeDto.MATCH
            else -> null
        }
    }

    fun asNftActivityType(source: com.rarible.protocol.union.dto.ActivityTypeDto): NftActivityFilterAllTypeDto? {
        return when (source) {
            com.rarible.protocol.union.dto.ActivityTypeDto.BURN -> NftActivityFilterAllTypeDto.BURN
            com.rarible.protocol.union.dto.ActivityTypeDto.MINT -> NftActivityFilterAllTypeDto.MINT
            com.rarible.protocol.union.dto.ActivityTypeDto.TRANSFER -> NftActivityFilterAllTypeDto.TRANSFER
            else -> null
        }
    }

    fun convertToNftTypes(types: List<com.rarible.protocol.union.dto.ActivityTypeDto>): List<NftActivityFilterAllTypeDto>? {
        val result = types.mapNotNull { asNftActivityType(it) }.distinct()
        return result.ifEmpty { null }
    }

    fun convertToOrderTypes(types: List<com.rarible.protocol.union.dto.ActivityTypeDto>): List<OrderActivityFilterAllTypeDto>? {
        val result = types.mapNotNull { asOrderActivityType(it) }.distinct()
        return result.ifEmpty { null }
    }

    fun convertToNftUserTypes(types: List<UserActivityTypeDto>): List<NftActivityFilterUserTypeDto>? {
        val result = types.mapNotNull { asUserNftActivityType(it) }.distinct()
        return result.ifEmpty { null }
    }

    fun convertToOrderUserTypes(types: List<UserActivityTypeDto>): List<OrderActivityFilterUserTypeDto>? {
        val result = types.mapNotNull { asUserOrderActivityType(it) }.distinct()
        return result.ifEmpty { null }
    }

    private suspend fun activityToSell(
        activity: OrderActivityMatchDto,
        blockchain: BlockchainDto,
        nft: OrderActivitySideMatchDto,
        payment: OrderActivitySideMatchDto,
        type: OrderMatchSellDto.Type,
        activityId: ActivityIdDto,
        date: Instant,
        source: OrderActivitySourceDto
    ): OrderMatchSellDto {
        val unionPayment = TezosConverter.convert(payment.asset, blockchain)
        val priceUsd = currencyService.toUsd(blockchain, unionPayment.type, activity.price, date)
        return OrderMatchSellDto(
            id = activityId,
            date = date,
            source = source,
            transactionHash = activity.transactionHash,
            // TODO UNION remove in 1.19
            blockchainInfo = asActivityBlockchainInfo(activity),
            nft = TezosConverter.convert(nft.asset, blockchain),
            payment = unionPayment,
            seller = UnionAddressConverter.convert(blockchain, nft.maker),
            buyer = UnionAddressConverter.convert(blockchain, payment.maker),
            sellerOrderHash = nft.hash,
            buyerOrderHash = payment.hash,
            price = activity.price,
            priceUsd = priceUsd,
            amountUsd = priceUsd?.multiply(nft.asset.value),
            type = type
        )
    }

    private fun activityToSwap(
        activity: OrderActivityMatchDto,
        blockchain: BlockchainDto,
        activityId: ActivityIdDto,
        date: Instant,
        source: OrderActivitySourceDto
    ) = OrderMatchSwapDto(
        id = activityId,
        date = date,
        source = source,
        transactionHash = activity.transactionHash,
        // TODO UNION remove in 1.19
        blockchainInfo = asActivityBlockchainInfo(activity),
        left = convert(activity.left, blockchain),
        right = convert(activity.right, blockchain)
    )

    private fun asActivityBlockchainInfo(source: OrderActivityMatchDto) = ActivityBlockchainInfoDto(
        transactionHash = source.transactionHash,
        blockHash = source.blockHash,
        blockNumber = source.blockNumber.toLong(),
        logIndex = source.logIndex
    )

    private fun convert(
        source: OrderActivitySideMatchDto,
        blockchain: BlockchainDto
    ): OrderActivityMatchSideDto {
        return OrderActivityMatchSideDto(
            maker = UnionAddressConverter.convert(blockchain, source.maker),
            hash = source.hash,
            asset = TezosConverter.convert(source.asset, blockchain)
        )
    }

    private fun convertSource(source: String): OrderActivitySourceDto {
        // At the moment Tezos supports only RARIBLE source
        if (source == OrderActivitySourceDto.RARIBLE.name) {
            return OrderActivitySourceDto.RARIBLE
        }
        throw IllegalArgumentException("Unsupported source of Tezos activity: $source")
    }

    private fun convertType(source: OrderActivityMatchTypeDto) =
        when (source) {
            OrderActivityMatchTypeDto.SELL -> OrderMatchSellDto.Type.SELL
            OrderActivityMatchTypeDto.ACCEPT_BID -> OrderMatchSellDto.Type.ACCEPT_BID
        }
}