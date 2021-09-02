package com.rarible.protocol.union.core.converter.ethereum

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*

object EthUnionActivityDtoConverter {

    fun convert(source: ActivityDto, blockchain: Blockchain): UnionActivityDto {
        return when (source) {
            is OrderActivityMatchDto -> {
                EthOrderMatchActivityDto(
                    blockchain = EthBlockchainConverter.convert(blockchain),
                    id = source.id,
                    date = source.date,
                    left = convert(source.left),
                    right = convert(source.right),
                    price = source.price,
                    priceUsd = source.priceUsd,
                    source = convert(source.source),
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthTypesConverter.convert(source.transactionHash),
                        blockHash = EthTypesConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is OrderActivityBidDto -> {
                EthOrderBidActivityDto(
                    blockchain = EthBlockchainConverter.convert(blockchain),
                    id = source.id,
                    date = source.date,
                    price = source.price,
                    priceUsd = source.priceUsd,
                    source = convert(source.source),
                    hash = EthTypesConverter.convert(source.hash),
                    maker = EthAddressConverter.convert(source.maker),
                    make = EthAssetDtoConverter.convert(source.make),
                    take = EthAssetDtoConverter.convert(source.take)
                )
            }
            is OrderActivityListDto -> {
                EthOrderListActivityDto(
                    blockchain = EthBlockchainConverter.convert(blockchain),
                    id = source.id,
                    date = source.date,
                    price = source.price,
                    priceUsd = source.priceUsd,
                    source = convert(source.source),
                    hash = EthTypesConverter.convert(source.hash),
                    maker = EthAddressConverter.convert(source.maker),
                    make = EthAssetDtoConverter.convert(source.make),
                    take = EthAssetDtoConverter.convert(source.take)
                )
            }
            is OrderActivityCancelBidDto -> {
                EthOrderCancelBidActivityDto(
                    blockchain = EthBlockchainConverter.convert(blockchain),
                    id = source.id,
                    date = source.date,
                    source = convert(source.source),
                    hash = EthTypesConverter.convert(source.hash),
                    maker = EthAddressConverter.convert(source.maker),
                    make = EthAssetTypeDtoConverter.convert(source.make),
                    take = EthAssetTypeDtoConverter.convert(source.take),
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthTypesConverter.convert(source.transactionHash),
                        blockHash = EthTypesConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is OrderActivityCancelListDto -> {
                EthOrderCancelListActivityDto(
                    blockchain = EthBlockchainConverter.convert(blockchain),
                    id = source.id,
                    date = source.date,
                    source = convert(source.source),
                    hash = EthTypesConverter.convert(source.hash),
                    maker = EthAddressConverter.convert(source.maker),
                    make = EthAssetTypeDtoConverter.convert(source.make),
                    take = EthAssetTypeDtoConverter.convert(source.take),
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthTypesConverter.convert(source.transactionHash),
                        blockHash = EthTypesConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is MintDto -> {
                EthMintActivityDto(
                    blockchain = EthBlockchainConverter.convert(blockchain),
                    id = source.id,
                    date = source.date,
                    owner = listOf(EthAddressConverter.convert(source.owner)),
                    contract = EthAddressConverter.convert(source.contract),
                    tokenId = source.tokenId,
                    value = source.value,
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthTypesConverter.convert(source.transactionHash),
                        blockHash = EthTypesConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is BurnDto -> {
                EthBurnActivityDto(
                    blockchain = EthBlockchainConverter.convert(blockchain),
                    id = source.id,
                    date = source.date,
                    owner = listOf(EthAddressConverter.convert(source.owner)),
                    contract = EthAddressConverter.convert(source.contract),
                    tokenId = source.tokenId,
                    value = source.value,
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthTypesConverter.convert(source.transactionHash),
                        blockHash = EthTypesConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is TransferDto -> {
                EthTransferActivityDto(
                    blockchain = EthBlockchainConverter.convert(blockchain),
                    id = source.id,
                    date = source.date,
                    from = EthAddressConverter.convert(source.from),
                    owner = listOf(EthAddressConverter.convert(source.owner)),
                    contract = EthAddressConverter.convert(source.contract),
                    tokenId = source.tokenId,
                    value = source.value,
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = EthTypesConverter.convert(source.transactionHash),
                        blockHash = EthTypesConverter.convert(source.blockHash),
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
        }
    }

    private fun convert(source: OrderActivityMatchSideDto): EthOrderActivityMatchSideDto {
        return EthOrderActivityMatchSideDto(
            maker = EthAddressConverter.convert(source.maker),
            hash = EthTypesConverter.convert(source.hash),
            asset = EthAssetDtoConverter.convert(source.asset),
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
        }
    }
}