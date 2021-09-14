package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*

object EthUnionActivityConverter {

    fun convert(source: ActivityDto, blockchain: EthBlockchainDto): UnionActivityDto {
        val unionActivityId = EthActivityIdDto(source.id, blockchain)
        return when (source) {
            is OrderActivityMatchDto -> {
                EthOrderMatchActivityDto(
                    id = unionActivityId,
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
                EthOrderBidActivityDto(
                    id = unionActivityId,
                    date = source.date,
                    price = source.price,
                    priceUsd = source.priceUsd,
                    source = convert(source.source),
                    hash = EthConverter.convert(source.hash),
                    maker = EthAddressConverter.convert(source.maker, blockchain),
                    make = EthConverter.convert(source.make, blockchain),
                    take = EthConverter.convert(source.take, blockchain)
                )
            }
            is OrderActivityListDto -> {
                EthOrderListActivityDto(
                    id = unionActivityId,
                    date = source.date,
                    price = source.price,
                    priceUsd = source.priceUsd,
                    source = convert(source.source),
                    hash = EthConverter.convert(source.hash),
                    maker = EthAddressConverter.convert(source.maker, blockchain),
                    make = EthConverter.convert(source.make, blockchain),
                    take = EthConverter.convert(source.take, blockchain)
                )
            }
            is OrderActivityCancelBidDto -> {
                EthOrderCancelBidActivityDto(
                    id = unionActivityId,
                    date = source.date,
                    source = convert(source.source),
                    hash = EthConverter.convert(source.hash),
                    maker = EthAddressConverter.convert(source.maker, blockchain),
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
                EthOrderCancelListActivityDto(
                    id = unionActivityId,
                    date = source.date,
                    source = convert(source.source),
                    hash = EthConverter.convert(source.hash),
                    maker = EthAddressConverter.convert(source.maker, blockchain),
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
                EthMintActivityDto(
                    id = unionActivityId,
                    date = source.date,
                    owners = listOf(EthAddressConverter.convert(source.owner, blockchain)),
                    contract = EthAddressConverter.convert(source.contract, blockchain),
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
                EthBurnActivityDto(
                    id = unionActivityId,
                    date = source.date,
                    owners = listOf(EthAddressConverter.convert(source.owner, blockchain)),
                    contract = EthAddressConverter.convert(source.contract, blockchain),
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
                EthTransferActivityDto(
                    id = unionActivityId,
                    date = source.date,
                    from = EthAddressConverter.convert(source.from, blockchain),
                    owners = listOf(EthAddressConverter.convert(source.owner, blockchain)),
                    contract = EthAddressConverter.convert(source.contract, blockchain),
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

    fun asUserActivityType(source: UnionUserActivityTypeDto): ActivityFilterByUserDto.Types {
        return when (source) {
            UnionUserActivityTypeDto.BURN -> ActivityFilterByUserDto.Types.BURN
            UnionUserActivityTypeDto.BUY -> ActivityFilterByUserDto.Types.BUY
            UnionUserActivityTypeDto.GET_BID -> ActivityFilterByUserDto.Types.GET_BID
            UnionUserActivityTypeDto.LIST -> ActivityFilterByUserDto.Types.LIST
            UnionUserActivityTypeDto.MAKE_BID -> ActivityFilterByUserDto.Types.MAKE_BID
            UnionUserActivityTypeDto.MINT -> ActivityFilterByUserDto.Types.MINT
            UnionUserActivityTypeDto.SELL -> ActivityFilterByUserDto.Types.SELL
            UnionUserActivityTypeDto.TRANSFER_FROM -> ActivityFilterByUserDto.Types.TRANSFER_FROM
            UnionUserActivityTypeDto.TRANSFER_TO -> ActivityFilterByUserDto.Types.TRANSFER_TO
        }
    }

    fun asItemActivityType(source: UnionActivityTypeDto): ActivityFilterByItemDto.Types {
        return when (source) {
            UnionActivityTypeDto.BID -> ActivityFilterByItemDto.Types.BID
            UnionActivityTypeDto.BURN -> ActivityFilterByItemDto.Types.BURN
            UnionActivityTypeDto.LIST -> ActivityFilterByItemDto.Types.LIST
            UnionActivityTypeDto.MINT -> ActivityFilterByItemDto.Types.MINT
            UnionActivityTypeDto.SELL -> ActivityFilterByItemDto.Types.MATCH
            UnionActivityTypeDto.TRANSFER -> ActivityFilterByItemDto.Types.TRANSFER
        }
    }

    fun asCollectionActivityType(source: UnionActivityTypeDto): ActivityFilterByCollectionDto.Types {
        return when (source) {
            UnionActivityTypeDto.BID -> ActivityFilterByCollectionDto.Types.BID
            UnionActivityTypeDto.BURN -> ActivityFilterByCollectionDto.Types.BURN
            UnionActivityTypeDto.LIST -> ActivityFilterByCollectionDto.Types.LIST
            UnionActivityTypeDto.MINT -> ActivityFilterByCollectionDto.Types.MINT
            UnionActivityTypeDto.SELL -> ActivityFilterByCollectionDto.Types.MATCH
            UnionActivityTypeDto.TRANSFER -> ActivityFilterByCollectionDto.Types.TRANSFER
        }
    }

    fun asGlobalActivityType(source: UnionActivityTypeDto): ActivityFilterAllDto.Types {
        return when (source) {
            UnionActivityTypeDto.BID -> ActivityFilterAllDto.Types.BID
            UnionActivityTypeDto.BURN -> ActivityFilterAllDto.Types.BURN
            UnionActivityTypeDto.LIST -> ActivityFilterAllDto.Types.LIST
            UnionActivityTypeDto.MINT -> ActivityFilterAllDto.Types.MINT
            UnionActivityTypeDto.SELL -> ActivityFilterAllDto.Types.SELL
            UnionActivityTypeDto.TRANSFER -> ActivityFilterAllDto.Types.TRANSFER
        }
    }

    private fun convert(source: OrderActivityMatchSideDto, blockchain: EthBlockchainDto): EthOrderActivityMatchSideDto {
        return EthOrderActivityMatchSideDto(
            maker = EthAddressConverter.convert(source.maker, blockchain),
            hash = EthConverter.convert(source.hash),
            asset = EthConverter.convert(source.asset, blockchain),
            type = convert(source.type!!) // TODO must be not null
        )
    }

    private fun convert(source: OrderActivityMatchSideDto.Type): EthOrderActivityMatchSideDto.Type {
        return when (source) {
            OrderActivityMatchSideDto.Type.BID -> EthOrderActivityMatchSideDto.Type.BID
            OrderActivityMatchSideDto.Type.SELL -> EthOrderActivityMatchSideDto.Type.SELL
        }
    }

    private fun convert(source: OrderActivityDto.Source): EthOrderActivitySourceDto {
        return when (source) {
            OrderActivityDto.Source.OPEN_SEA -> EthOrderActivitySourceDto.OPEN_SEA
            OrderActivityDto.Source.RARIBLE -> EthOrderActivitySourceDto.RARIBLE
            OrderActivityDto.Source.CRYPTO_PUNKS -> EthOrderActivitySourceDto.CRYPTO_PUNKS
        }
    }

}