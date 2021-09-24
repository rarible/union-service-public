package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.*
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.*

object EthUnionActivityConverter {

    fun convert(source: ActivityDto, blockchain: BlockchainDto): UnionActivityDto {
        val unionActivityId = UnionActivityIdDto(blockchain, source.id)
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
                    maker = UnionAddressConverter.convert(source.maker, blockchain),
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
                    maker = UnionAddressConverter.convert(source.maker, blockchain),
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
                EthOrderCancelListActivityDto(
                    id = unionActivityId,
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
                EthMintActivityDto(
                    id = unionActivityId,
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
                EthBurnActivityDto(
                    id = unionActivityId,
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
                EthTransferActivityDto(
                    id = unionActivityId,
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

    fun asUserActivityType(source: UnionUserActivityTypeDto): ActivityFilterByUserTypeDto {
        return when (source) {
            UnionUserActivityTypeDto.BURN -> ActivityFilterByUserTypeDto.BURN
            UnionUserActivityTypeDto.BUY -> ActivityFilterByUserTypeDto.BUY
            UnionUserActivityTypeDto.GET_BID -> ActivityFilterByUserTypeDto.GET_BID
            UnionUserActivityTypeDto.LIST -> ActivityFilterByUserTypeDto.LIST
            UnionUserActivityTypeDto.MAKE_BID -> ActivityFilterByUserTypeDto.MAKE_BID
            UnionUserActivityTypeDto.MINT -> ActivityFilterByUserTypeDto.MINT
            UnionUserActivityTypeDto.SELL -> ActivityFilterByUserTypeDto.SELL
            UnionUserActivityTypeDto.TRANSFER_FROM -> ActivityFilterByUserTypeDto.TRANSFER_FROM
            UnionUserActivityTypeDto.TRANSFER_TO -> ActivityFilterByUserTypeDto.TRANSFER_TO
        }
    }

    fun asItemActivityType(source: UnionActivityTypeDto): ActivityFilterByItemTypeDto {
        return when (source) {
            UnionActivityTypeDto.BID -> ActivityFilterByItemTypeDto.BID
            UnionActivityTypeDto.BURN -> ActivityFilterByItemTypeDto.BURN
            UnionActivityTypeDto.LIST -> ActivityFilterByItemTypeDto.LIST
            UnionActivityTypeDto.MINT -> ActivityFilterByItemTypeDto.MINT
            UnionActivityTypeDto.SELL -> ActivityFilterByItemTypeDto.MATCH
            UnionActivityTypeDto.TRANSFER -> ActivityFilterByItemTypeDto.TRANSFER
        }
    }

    fun asCollectionActivityType(source: UnionActivityTypeDto): ActivityFilterByCollectionTypeDto {
        return when (source) {
            UnionActivityTypeDto.BID -> ActivityFilterByCollectionTypeDto.BID
            UnionActivityTypeDto.BURN -> ActivityFilterByCollectionTypeDto.BURN
            UnionActivityTypeDto.LIST -> ActivityFilterByCollectionTypeDto.LIST
            UnionActivityTypeDto.MINT -> ActivityFilterByCollectionTypeDto.MINT
            UnionActivityTypeDto.SELL -> ActivityFilterByCollectionTypeDto.MATCH
            UnionActivityTypeDto.TRANSFER -> ActivityFilterByCollectionTypeDto.TRANSFER
        }
    }

    fun asGlobalActivityType(source: UnionActivityTypeDto): ActivityFilterAllTypeDto {
        return when (source) {
            UnionActivityTypeDto.BID -> ActivityFilterAllTypeDto.BID
            UnionActivityTypeDto.BURN -> ActivityFilterAllTypeDto.BURN
            UnionActivityTypeDto.LIST -> ActivityFilterAllTypeDto.LIST
            UnionActivityTypeDto.MINT -> ActivityFilterAllTypeDto.MINT
            UnionActivityTypeDto.SELL -> ActivityFilterAllTypeDto.SELL
            UnionActivityTypeDto.TRANSFER -> ActivityFilterAllTypeDto.TRANSFER
        }
    }

    private fun convert(source: OrderActivityMatchSideDto, blockchain: BlockchainDto): EthOrderActivityMatchSideDto {
        return EthOrderActivityMatchSideDto(
            maker = UnionAddressConverter.convert(source.maker, blockchain),
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