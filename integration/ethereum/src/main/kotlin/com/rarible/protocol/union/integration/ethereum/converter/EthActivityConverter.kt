package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.ActivityFilterAllTypeDto
import com.rarible.protocol.dto.ActivityFilterByCollectionTypeDto
import com.rarible.protocol.dto.ActivityFilterByItemTypeDto
import com.rarible.protocol.dto.ActivityFilterByUserTypeDto
import com.rarible.protocol.dto.BurnDto
import com.rarible.protocol.dto.MintDto
import com.rarible.protocol.dto.OrderActivityBidDto
import com.rarible.protocol.dto.OrderActivityCancelBidDto
import com.rarible.protocol.dto.OrderActivityCancelListDto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.OrderActivityListDto
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.TransferDto
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivityTypeDto
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
import org.springframework.stereotype.Component

@Component
class EthActivityConverter(
    private val currencyService: CurrencyService
) {

    // For activities, we decided to do NOT convert price to USD in realtime,
    // since we want to see here USD price for the moment when event happened
    // For example, if user bought NFT for 1 ETH 2 days ago, we want to see here USD price
    // for THAT date instead of price calculated for current rate
    suspend fun convert(source: com.rarible.protocol.dto.ActivityDto, blockchain: BlockchainDto): ActivityDto {
        val activityId = ActivityIdDto(blockchain, source.id)
        return when (source) {
            is OrderActivityMatchDto -> {
                val type = source.type
                val leftSide = source.left
                val rightSide = source.right
                val leftTypeExt = EthConverter.convert(leftSide.asset.assetType, blockchain).ext
                val rightTypeExt = EthConverter.convert(rightSide.asset.assetType, blockchain).ext
                if (type != null && leftTypeExt.isNft && rightTypeExt.isCurrency) {
                    activityToSell(
                        activityId = activityId,
                        source = source,
                        blockchain = blockchain,
                        nft = leftSide,
                        payment = rightSide,
                        type = convert(type)
                    )
                } else if (type != null && leftTypeExt.isCurrency && rightTypeExt.isNft) {
                    activityToSell(
                        activityId = activityId,
                        source = source,
                        blockchain = blockchain,
                        nft = rightSide,
                        payment = leftSide,
                        type = convert(type)
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
                val payment = EthConverter.convert(source.make, blockchain)
                val nft = EthConverter.convert(source.take, blockchain)
                OrderBidActivityDto(
                    id = activityId,
                    date = source.date,
                    price = source.price,
                    priceUsd = source.priceUsd,
                    //priceUsd = currencyService.toUsd(blockchain, payment.type.ext.contract, source.price),
                    source = convert(source.source),
                    hash = EthConverter.convert(source.hash),
                    maker = EthConverter.convert(source.maker, blockchain),
                    make = payment,
                    take = nft
                )
            }
            is OrderActivityListDto -> {
                val payment = EthConverter.convert(source.take, blockchain)
                val nft = EthConverter.convert(source.make, blockchain)
                OrderListActivityDto(
                    id = activityId,
                    date = source.date,
                    price = source.price,
                    priceUsd = source.priceUsd,
                    //priceUsd = currencyService.toUsd(blockchain, payment.type.ext.contract, source.price),
                    source = convert(source.source),
                    hash = EthConverter.convert(source.hash),
                    maker = EthConverter.convert(source.maker, blockchain),
                    make = nft,
                    take = payment
                )
            }
            is OrderActivityCancelBidDto -> {
                OrderCancelBidActivityDto(
                    id = activityId,
                    date = source.date,
                    source = convert(source.source),
                    hash = EthConverter.convert(source.hash),
                    maker = EthConverter.convert(source.maker, blockchain),
                    make = EthConverter.convert(source.make, blockchain),
                    take = EthConverter.convert(source.take, blockchain),
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthConverter.convert(source.transactionHash),
                        blockHash = EthConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is OrderActivityCancelListDto -> {
                OrderCancelListActivityDto(
                    id = activityId,
                    date = source.date,
                    source = convert(source.source),
                    hash = EthConverter.convert(source.hash),
                    maker = EthConverter.convert(source.maker, blockchain),
                    make = EthConverter.convert(source.make, blockchain),
                    take = EthConverter.convert(source.take, blockchain),
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthConverter.convert(source.transactionHash),
                        blockHash = EthConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is MintDto -> {
                MintActivityDto(
                    id = activityId,
                    date = source.date,
                    owner = EthConverter.convert(source.owner, blockchain),
                    contract = EthConverter.convert(source.contract, blockchain),
                    tokenId = source.tokenId,
                    value = source.value,
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthConverter.convert(source.transactionHash),
                        blockHash = EthConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is BurnDto -> {
                BurnActivityDto(
                    id = activityId,
                    date = source.date,
                    owner = EthConverter.convert(source.owner, blockchain),
                    contract = EthConverter.convert(source.contract, blockchain),
                    tokenId = source.tokenId,
                    value = source.value,
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthConverter.convert(source.transactionHash),
                        blockHash = EthConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is TransferDto -> {
                TransferActivityDto(
                    id = activityId,
                    date = source.date,
                    from = EthConverter.convert(source.from, blockchain),
                    owner = EthConverter.convert(source.owner, blockchain),
                    contract = EthConverter.convert(source.contract, blockchain),
                    tokenId = source.tokenId,
                    value = source.value,
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthConverter.convert(source.transactionHash),
                        blockHash = EthConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            else -> TODO() //TODO Implement Auction activities
        }
    }

    private suspend fun activityToSell(
        source: OrderActivityMatchDto,
        blockchain: BlockchainDto,
        nft: com.rarible.protocol.dto.OrderActivityMatchSideDto,
        payment: com.rarible.protocol.dto.OrderActivityMatchSideDto,
        type: OrderMatchSellDto.Type,
        activityId: ActivityIdDto
    ): OrderMatchSellDto {
        val unionPayment = EthConverter.convert(payment.asset, blockchain)
        val priceUsd = source.priceUsd
        //val priceUsd = currencyService.toUsd(blockchain, unionPayment.type.ext.contract, source.price)
        return OrderMatchSellDto(
            id = activityId,
            date = source.date,
            source = convert(source.source),
            blockchainInfo = asActivityBlockchainInfo(source),
            nft = EthConverter.convert(nft.asset, blockchain),
            payment = unionPayment,
            seller = EthConverter.convert(nft.maker, blockchain),
            buyer = EthConverter.convert(payment.maker, blockchain),
            price = source.price,
            priceUsd = priceUsd,
            amountUsd = priceUsd?.multiply(nft.asset.valueDecimal),
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
        source = convert(source.source),
        blockchainInfo = asActivityBlockchainInfo(source),
        left = convert(source.left, blockchain),
        right = convert(source.right, blockchain)
    )

    fun asUserActivityType(source: UserActivityTypeDto): ActivityFilterByUserTypeDto {
        return when (source) {
            UserActivityTypeDto.BURN -> ActivityFilterByUserTypeDto.BURN
            UserActivityTypeDto.BUY -> ActivityFilterByUserTypeDto.BUY
            UserActivityTypeDto.GET_BID -> ActivityFilterByUserTypeDto.GET_BID
            UserActivityTypeDto.LIST -> ActivityFilterByUserTypeDto.LIST
            UserActivityTypeDto.MAKE_BID -> ActivityFilterByUserTypeDto.MAKE_BID
            UserActivityTypeDto.MINT -> ActivityFilterByUserTypeDto.MINT
            UserActivityTypeDto.SELL -> ActivityFilterByUserTypeDto.SELL
            UserActivityTypeDto.TRANSFER_FROM -> ActivityFilterByUserTypeDto.TRANSFER_FROM
            UserActivityTypeDto.TRANSFER_TO -> ActivityFilterByUserTypeDto.TRANSFER_TO
        }
    }

    fun asItemActivityType(source: ActivityTypeDto): ActivityFilterByItemTypeDto {
        return when (source) {
            ActivityTypeDto.BID -> ActivityFilterByItemTypeDto.BID
            ActivityTypeDto.BURN -> ActivityFilterByItemTypeDto.BURN
            ActivityTypeDto.LIST -> ActivityFilterByItemTypeDto.LIST
            ActivityTypeDto.MINT -> ActivityFilterByItemTypeDto.MINT
            ActivityTypeDto.SELL -> ActivityFilterByItemTypeDto.MATCH
            ActivityTypeDto.TRANSFER -> ActivityFilterByItemTypeDto.TRANSFER
        }
    }

    fun asCollectionActivityType(source: ActivityTypeDto): ActivityFilterByCollectionTypeDto {
        return when (source) {
            ActivityTypeDto.BID -> ActivityFilterByCollectionTypeDto.BID
            ActivityTypeDto.BURN -> ActivityFilterByCollectionTypeDto.BURN
            ActivityTypeDto.LIST -> ActivityFilterByCollectionTypeDto.LIST
            ActivityTypeDto.MINT -> ActivityFilterByCollectionTypeDto.MINT
            ActivityTypeDto.SELL -> ActivityFilterByCollectionTypeDto.MATCH
            ActivityTypeDto.TRANSFER -> ActivityFilterByCollectionTypeDto.TRANSFER
        }
    }

    fun asGlobalActivityType(source: ActivityTypeDto): ActivityFilterAllTypeDto {
        return when (source) {
            ActivityTypeDto.BID -> ActivityFilterAllTypeDto.BID
            ActivityTypeDto.BURN -> ActivityFilterAllTypeDto.BURN
            ActivityTypeDto.LIST -> ActivityFilterAllTypeDto.LIST
            ActivityTypeDto.MINT -> ActivityFilterAllTypeDto.MINT
            ActivityTypeDto.SELL -> ActivityFilterAllTypeDto.SELL
            ActivityTypeDto.TRANSFER -> ActivityFilterAllTypeDto.TRANSFER
        }
    }

    private fun asActivityBlockchainInfo(source: OrderActivityMatchDto) = ActivityBlockchainInfoDto(
        transactionHash = EthConverter.convert(source.transactionHash),
        blockHash = EthConverter.convert(source.blockHash),
        blockNumber = source.blockNumber,
        logIndex = source.logIndex
    )

    private fun convert(
        source: com.rarible.protocol.dto.OrderActivityMatchSideDto,
        blockchain: BlockchainDto
    ): OrderActivityMatchSideDto {
        return OrderActivityMatchSideDto(
            maker = EthConverter.convert(source.maker, blockchain),
            hash = EthConverter.convert(source.hash),
            asset = EthConverter.convert(source.asset, blockchain)
        )
    }

    private fun convert(source: OrderActivityDto.Source): OrderActivitySourceDto {
        return when (source) {
            OrderActivityDto.Source.OPEN_SEA -> OrderActivitySourceDto.OPEN_SEA
            OrderActivityDto.Source.RARIBLE -> OrderActivitySourceDto.RARIBLE
            OrderActivityDto.Source.CRYPTO_PUNKS -> OrderActivitySourceDto.CRYPTO_PUNKS
        }
    }

    private fun convert(source: OrderActivityMatchDto.Type) =
        when (source) {
            OrderActivityMatchDto.Type.SELL -> OrderMatchSellDto.Type.SELL
            OrderActivityMatchDto.Type.ACCEPT_BID -> OrderMatchSellDto.Type.ACCEPT_BID
        }
}

