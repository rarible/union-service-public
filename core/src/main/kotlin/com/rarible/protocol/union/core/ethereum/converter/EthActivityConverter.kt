package com.rarible.protocol.union.core.ethereum.converter

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
import com.rarible.protocol.union.core.converter.UnionAddressConverter
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
import com.rarible.protocol.union.dto.OrderMatchActivityDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UserActivityTypeDto

object EthActivityConverter {

    fun convert(source: com.rarible.protocol.dto.ActivityDto, blockchain: BlockchainDto): ActivityDto {
        val activityId = ActivityIdDto(blockchain, source.id)
        return when (source) {
            is OrderActivityMatchDto -> {
                OrderMatchActivityDto(
                    id = activityId,
                    date = source.date,
                    left = convert(source.left, blockchain),
                    right = convert(source.right, blockchain),
                    price = source.price,
                    priceUsd = source.priceUsd,
                    source = convert(source.source),
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthConverter.convert(source.transactionHash),
                        blockHash = EthConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is OrderActivityBidDto -> {
                OrderBidActivityDto(
                    id = activityId,
                    date = source.date,
                    price = source.price,
                    priceUsd = source.priceUsd,
                    source = convert(source.source),
                    hash = EthConverter.convert(source.hash),
                    maker = UnionAddressConverter.convert(source.maker, blockchain),
                    make = EthConverter.convert(source.make, blockchain),
                    take = EthConverter.convert(source.take, blockchain)
                )
            }
            is OrderActivityListDto -> {
                OrderListActivityDto(
                    id = activityId,
                    date = source.date,
                    price = source.price,
                    priceUsd = source.priceUsd,
                    source = convert(source.source),
                    hash = EthConverter.convert(source.hash),
                    maker = UnionAddressConverter.convert(source.maker, blockchain),
                    make = EthConverter.convert(source.make, blockchain),
                    take = EthConverter.convert(source.take, blockchain)
                )
            }
            is OrderActivityCancelBidDto -> {
                OrderCancelBidActivityDto(
                    id = activityId,
                    date = source.date,
                    source = convert(source.source),
                    hash = EthConverter.convert(source.hash),
                    maker = UnionAddressConverter.convert(source.maker, blockchain),
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
                    maker = UnionAddressConverter.convert(source.maker, blockchain),
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
                    owners = listOf(UnionAddressConverter.convert(source.owner, blockchain)),
                    contract = UnionAddressConverter.convert(source.contract, blockchain),
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
                    owners = listOf(UnionAddressConverter.convert(source.owner, blockchain)),
                    contract = UnionAddressConverter.convert(source.contract, blockchain),
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
                    from = UnionAddressConverter.convert(source.from, blockchain),
                    owners = listOf(UnionAddressConverter.convert(source.owner, blockchain)),
                    contract = UnionAddressConverter.convert(source.contract, blockchain),
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
        }
    }

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

    private fun convert(
        source: com.rarible.protocol.dto.OrderActivityMatchSideDto,
        blockchain: BlockchainDto
    ): OrderActivityMatchSideDto {
        return OrderActivityMatchSideDto(
            maker = UnionAddressConverter.convert(source.maker, blockchain),
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

}