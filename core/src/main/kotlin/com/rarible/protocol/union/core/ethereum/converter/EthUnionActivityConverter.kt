package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*

object EthUnionActivityConverter {

    fun convert(source: ActivityDto, blockchain: EthBlockchainDto): UnionActivityDto {
        return when (source) {
            is OrderActivityMatchDto -> {
                EthOrderMatchActivityDto(
                    blockchain = blockchain,
                    id = source.id,
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
                    blockchain = blockchain,
                    id = source.id,
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
                    blockchain = blockchain,
                    id = source.id,
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
                    blockchain = blockchain,
                    id = source.id,
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
                    blockchain = blockchain,
                    id = source.id,
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
                    blockchain = blockchain,
                    id = source.id,
                    date = source.date,
                    owner = listOf(EthAddressConverter.convert(source.owner, blockchain)),
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
                    blockchain = blockchain,
                    id = source.id,
                    date = source.date,
                    owner = listOf(EthAddressConverter.convert(source.owner, blockchain)),
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
                    blockchain = blockchain,
                    id = source.id,
                    date = source.date,
                    from = EthAddressConverter.convert(source.from, blockchain),
                    owner = listOf(EthAddressConverter.convert(source.owner, blockchain)),
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

    fun convert(source: OrderActivityMatchSideDto, blockchain: EthBlockchainDto): EthOrderActivityMatchSideDto {
        return EthOrderActivityMatchSideDto(
            maker = EthAddressConverter.convert(source.maker, blockchain),
            hash = EthConverter.convert(source.hash),
            asset = EthConverter.convert(source.asset, blockchain),
            type = convert(source.type!!) // TODO must be not null
        )
    }

    fun convert(source: OrderActivityMatchSideDto.Type): EthOrderActivityMatchSideDto.Type {
        return when (source) {
            OrderActivityMatchSideDto.Type.BID -> EthOrderActivityMatchSideDto.Type.BID
            OrderActivityMatchSideDto.Type.SELL -> EthOrderActivityMatchSideDto.Type.SELL
        }
    }

    fun convert(source: OrderActivityDto.Source): EthOrderActivitySourceDto {
        return when (source) {
            OrderActivityDto.Source.OPEN_SEA -> EthOrderActivitySourceDto.OPEN_SEA
            OrderActivityDto.Source.RARIBLE -> EthOrderActivitySourceDto.RARIBLE
        }
    }

}