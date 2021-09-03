package com.rarible.protocol.union.core.converter.flow

import com.rarible.protocol.dto.*
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowOrderActivityMatchSideDto
import com.rarible.protocol.union.dto.*
import org.springframework.core.convert.converter.Converter

object FlowUnionActivityDtoConverter : Converter<FlowActivityDto, UnionActivityDto> {

    override fun convert(source: FlowActivityDto): UnionActivityDto {
        return when (source) {
            is FlowNftOrderActivitySellDto -> {
                FlowOrderMatchActivityDto(
                    blockchain = FlowBlockchainConverter.convert(),
                    id = source.id,
                    date = source.date,
                    left = convert(source.left),
                    right = convert(source.right),
                    price = source.price,
                    priceUsd = source.price, // TODO should be in USD
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash,
                        blockHash = source.blockHash,
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is FlowNftOrderActivityListDto -> {
                FlowOrderListActivityDto(
                    blockchain = FlowBlockchainConverter.convert(),
                    id = source.id,
                    date = source.date,
                    price = source.price,
                    priceUsd = source.price, // TODO should be in USD
                    hash = source.hash,
                    maker = FlowAddressConverter.convert(source.maker),
                    make = FlowAssetDtoConverter.convert(source.make),
                    take = FlowAssetDtoConverter.convert(source.take)
                )
            }
            is FlowNftOrderActivityCancelListDto -> {
                FlowOrderCancelListActivityDto(
                    blockchain = FlowBlockchainConverter.convert(),
                    id = source.id,
                    date = source.date,
                    hash = source.hash,
                    maker = FlowAddressConverter.convert(source.maker),
                    make = FlowAssetDtoConverter.convert(source.make),
                    take = FlowAssetDtoConverter.convert(source.take)
                )
            }
            is FlowMintDto -> {
                FlowMintActivityDto(
                    blockchain = FlowBlockchainConverter.convert(),
                    id = source.id,
                    date = source.date,
                    owner = listOf(FlowAddressConverter.convert(source.owner)),
                    contract = FlowContractConverter.convert(source.contract),
                    tokenId = source.tokenId,
                    value = source.value,
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash,
                        blockHash = source.blockHash,
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is FlowBurnDto -> {
                FlowBurnActivityDto(
                    blockchain = FlowBlockchainConverter.convert(),
                    id = source.id,
                    date = source.date,
                    owner = listOf(FlowAddressConverter.convert(source.owner)),
                    contract = FlowContractConverter.convert(source.contract),
                    tokenId = source.tokenId,
                    value = source.value,
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash,
                        blockHash = source.blockHash,
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is FlowTransferDto -> {
                FlowTransferActivityDto(
                    blockchain = FlowBlockchainConverter.convert(),
                    id = source.id,
                    date = source.date,
                    from = FlowAddressConverter.convert(source.from),
                    owner = listOf(FlowAddressConverter.convert(source.owner)),
                    contract = FlowContractConverter.convert(source.contract),
                    tokenId = source.tokenId,
                    value = source.value,
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash,
                        blockHash = source.blockHash,
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
        }
    }

    private fun convert(source: FlowOrderActivityMatchSideDto): com.rarible.protocol.union.dto.FlowOrderActivityMatchSideDto {
        return com.rarible.protocol.union.dto.FlowOrderActivityMatchSideDto(
            maker = FlowAddress(source.maker),
            asset = FlowAssetDtoConverter.convert(source.asset),
            type = convert(source.type)
        )
    }

    private fun convert(source: FlowOrderActivityMatchSideDto.Type): com.rarible.protocol.union.dto.FlowOrderActivityMatchSideDto.Type {
        return when (source) {
            FlowOrderActivityMatchSideDto.Type.BID -> com.rarible.protocol.union.dto.FlowOrderActivityMatchSideDto.Type.BID
            FlowOrderActivityMatchSideDto.Type.SELL -> com.rarible.protocol.union.dto.FlowOrderActivityMatchSideDto.Type.SELL
        }
    }

    private fun convert(source: OrderActivityDto.Source): EthOrderActivitySourceDto {
        return when (source) {
            OrderActivityDto.Source.OPEN_SEA -> EthOrderActivitySourceDto.OPEN_SEA
            OrderActivityDto.Source.RARIBLE -> EthOrderActivitySourceDto.RARIBLE
        }
    }
}