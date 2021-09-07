package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.*
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.FlowOrderActivityMatchSideDto

object FlowUnionActivityConverter {

    fun convert(source: FlowActivityDto, blockchain: FlowBlockchainDto): UnionActivityDto {
        return when (source) {
            is FlowNftOrderActivitySellDto -> {
                FlowOrderMatchActivityDto(
                    blockchain = blockchain,
                    id = source.id,
                    date = source.date,
                    left = convert(source.left, blockchain),
                    right = convert(source.right, blockchain),
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
                    blockchain = blockchain,
                    id = source.id,
                    date = source.date,
                    price = source.price,
                    priceUsd = source.price, // TODO should be in USD
                    hash = source.hash,
                    maker = FlowAddressConverter.convert(source.maker, blockchain),
                    make = FlowConverter.convert(source.make, blockchain),
                    take = FlowConverter.convert(source.take, blockchain)
                )
            }
            is FlowNftOrderActivityCancelListDto -> {
                FlowOrderCancelListActivityDto(
                    blockchain = blockchain,
                    id = source.id,
                    date = source.date,
                    hash = source.hash,
                    maker = FlowAddressConverter.convert(source.maker, blockchain),
                    make = FlowConverter.convert(source.make, blockchain),
                    take = FlowConverter.convert(source.take, blockchain)
                )
            }
            is FlowMintDto -> {
                FlowMintActivityDto(
                    blockchain = blockchain,
                    id = source.id,
                    date = source.date,
                    owner = listOf(FlowAddressConverter.convert(source.owner, blockchain)),
                    contract = FlowContractConverter.convert(source.contract, blockchain),
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
                    blockchain = blockchain,
                    id = source.id,
                    date = source.date,
                    owner = listOf(FlowAddressConverter.convert(source.owner, blockchain)),
                    contract = FlowContractConverter.convert(source.contract, blockchain),
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
                    blockchain = blockchain,
                    id = source.id,
                    date = source.date,
                    from = FlowAddressConverter.convert(source.from, blockchain),
                    owner = listOf(FlowAddressConverter.convert(source.owner, blockchain)),
                    contract = FlowContractConverter.convert(source.contract, blockchain),
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

    private fun convert(
        source: com.rarible.protocol.dto.FlowOrderActivityMatchSideDto,
        blockchain: FlowBlockchainDto
    ): FlowOrderActivityMatchSideDto {
        return FlowOrderActivityMatchSideDto(
            maker = FlowAddressConverter.convert(source.maker, blockchain),
            asset = FlowConverter.convert(source.asset, blockchain),
            type = convert(source.type)
        )
    }

    private fun convert(source: com.rarible.protocol.dto.FlowOrderActivityMatchSideDto.Type): FlowOrderActivityMatchSideDto.Type {
        return when (source) {
            com.rarible.protocol.dto.FlowOrderActivityMatchSideDto.Type.BID -> FlowOrderActivityMatchSideDto.Type.BID
            com.rarible.protocol.dto.FlowOrderActivityMatchSideDto.Type.SELL -> FlowOrderActivityMatchSideDto.Type.SELL
        }
    }

}