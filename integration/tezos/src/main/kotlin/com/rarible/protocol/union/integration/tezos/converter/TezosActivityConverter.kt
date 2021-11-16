package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.tezos.dto.ActivityTypeDto
import com.rarible.protocol.tezos.dto.BurnDto
import com.rarible.protocol.tezos.dto.MintDto
import com.rarible.protocol.tezos.dto.NftActivityFilterAllTypeDto
import com.rarible.protocol.tezos.dto.NftActivityFilterUserTypeDto
import com.rarible.protocol.tezos.dto.OrderActivityBidDto
import com.rarible.protocol.tezos.dto.OrderActivityCancelBidDto
import com.rarible.protocol.tezos.dto.OrderActivityCancelListDto
import com.rarible.protocol.tezos.dto.OrderActivityFilterAllTypeDto
import com.rarible.protocol.tezos.dto.OrderActivityFilterUserTypeDto
import com.rarible.protocol.tezos.dto.OrderActivityListDto
import com.rarible.protocol.tezos.dto.OrderActivityMatchDto
import com.rarible.protocol.tezos.dto.OrderActivitySideMatchDto
import com.rarible.protocol.tezos.dto.TransferDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.ContractAddress
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
import org.springframework.stereotype.Component

@Component
class TezosActivityConverter(
    private val currencyService: CurrencyService
) {

    suspend fun convert(source: ActivityTypeDto, blockchain: BlockchainDto): ActivityDto {
        val activityId = ActivityIdDto(blockchain, source.id)
        return when (source) {
            is OrderActivityMatchDto -> {
                // TODO TEZOS add type sell/accept_bid
                //val type = source.type
                val type: String? = null
                val leftSide = source.left
                val rightSide = source.right
                val leftTypeExt = TezosConverter.convert(leftSide.asset.assetType, blockchain).ext
                val rightTypeExt = TezosConverter.convert(rightSide.asset.assetType, blockchain).ext
                if (type != null && leftTypeExt.isNft && rightTypeExt.isCurrency) {
                    activityToSell(
                        activityId = activityId,
                        source = source,
                        blockchain = blockchain,
                        nft = leftSide,
                        payment = rightSide,
                        type = convertType(type)
                    )
                } else if (type != null && leftTypeExt.isCurrency && rightTypeExt.isNft) {
                    activityToSell(
                        activityId = activityId,
                        source = source,
                        blockchain = blockchain,
                        nft = rightSide,
                        payment = leftSide,
                        type = convertType(type)
                    )
                } else {
                    activityToSwap(
                        activityId = activityId,
                        source = source,
                        blockchain = blockchain
                    )
                }
            }
            is OrderActivityBidDto -> {
                val payment = TezosConverter.convert(source.make, blockchain)
                val nft = TezosConverter.convert(source.take, blockchain)
                OrderBidActivityDto(
                    id = activityId,
                    date = source.date,
                    price = source.price,
                    priceUsd = currencyService.toUsd(blockchain, payment.type, source.price, source.date),
                    source = convertSource(source.source),
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(blockchain, source.maker),
                    make = payment,
                    take = nft
                )
            }
            is OrderActivityListDto -> {
                val payment = TezosConverter.convert(source.take, blockchain)
                val nft = TezosConverter.convert(source.make, blockchain)
                OrderListActivityDto(
                    id = activityId,
                    date = source.date,
                    price = source.price,
                    priceUsd = currencyService.toUsd(blockchain, payment.type, source.price, source.date),
                    source = convertSource(source.source),
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(blockchain, source.maker),
                    make = nft,
                    take = payment
                )
            }
            is OrderActivityCancelBidDto -> {
                OrderCancelBidActivityDto(
                    id = activityId,
                    date = source.date,
                    source = convertSource(source.source),
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(blockchain, source.maker),
                    make = TezosConverter.convert(source.make, blockchain),
                    take = TezosConverter.convert(source.take, blockchain),
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash,
                        blockHash = source.blockHash,
                        blockNumber = source.blockNumber.toLong(),
                        logIndex = source.logIndex
                    )
                )
            }
            is OrderActivityCancelListDto -> {
                OrderCancelListActivityDto(
                    id = activityId,
                    date = source.date,
                    source = convertSource(source.source),
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(blockchain, source.maker),
                    make = TezosConverter.convert(source.make, blockchain),
                    take = TezosConverter.convert(source.take, blockchain),
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash,
                        blockHash = source.blockHash,
                        blockNumber = source.blockNumber.toLong(),
                        logIndex = source.logIndex
                    )
                )
            }

            is MintDto -> {
                MintActivityDto(
                    id = activityId,
                    date = source.date,
                    owner = UnionAddressConverter.convert(blockchain, source.owner),
                    contract = ContractAddress(blockchain, source.contract),
                    tokenId = source.tokenId,
                    value = source.value,
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash,
                        blockHash = source.blockHash,
                        blockNumber = source.blockNumber.toLong(),
                        logIndex = source.logIndex
                    )
                )
            }
            is BurnDto -> {
                BurnActivityDto(
                    id = activityId,
                    date = source.date,
                    owner = UnionAddressConverter.convert(blockchain, source.owner),
                    contract = ContractAddress(blockchain, source.contract),
                    tokenId = source.tokenId,
                    value = source.value,
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash,
                        blockHash = source.blockHash,
                        blockNumber = source.blockNumber.toLong(),
                        logIndex = source.logIndex
                    )
                )
            }
            is TransferDto -> {
                TransferActivityDto(
                    id = activityId,
                    date = source.date,
                    from = UnionAddressConverter.convert(blockchain, source.from),
                    owner = UnionAddressConverter.convert(blockchain, source.elt.owner),
                    contract = ContractAddress(blockchain, source.elt.contract),
                    tokenId = source.elt.tokenId,
                    value = source.elt.value,
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.elt.transactionHash,
                        blockHash = source.elt.blockHash,
                        blockNumber = source.elt.blockNumber.toLong(),
                        logIndex = source.elt.logIndex
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
        source: OrderActivityMatchDto,
        blockchain: BlockchainDto,
        nft: OrderActivitySideMatchDto,
        payment: OrderActivitySideMatchDto,
        type: OrderMatchSellDto.Type,
        activityId: ActivityIdDto
    ): OrderMatchSellDto {
        val unionPayment = TezosConverter.convert(payment.asset, blockchain)
        val priceUsd = currencyService.toUsd(blockchain, unionPayment.type, source.price, source.date)
        return OrderMatchSellDto(
            id = activityId,
            date = source.date,
            source = convertSource(source.source),
            blockchainInfo = asActivityBlockchainInfo(source),
            nft = TezosConverter.convert(nft.asset, blockchain),
            payment = unionPayment,
            seller = UnionAddressConverter.convert(blockchain, nft.maker),
            buyer = UnionAddressConverter.convert(blockchain, payment.maker),
            price = source.price,
            priceUsd = priceUsd,
            amountUsd = priceUsd?.multiply(nft.asset.value),
            type = type
        )
    }

    private fun activityToSwap(
        source: OrderActivityMatchDto,
        blockchain: BlockchainDto,
        activityId: ActivityIdDto
    ) = OrderMatchSwapDto(
        id = activityId,
        date = source.date,
        source = convertSource(source.source),
        blockchainInfo = asActivityBlockchainInfo(source),
        left = convert(source.left, blockchain),
        right = convert(source.right, blockchain)
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

    private fun convertType(source: String) =
        //TODO add correct converter
        OrderMatchSellDto.Type.valueOf(source)
}