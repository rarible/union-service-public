package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowBurnDto
import com.rarible.protocol.dto.FlowMintDto
import com.rarible.protocol.dto.FlowNftOrderActivityCancelListDto
import com.rarible.protocol.dto.FlowNftOrderActivityListDto
import com.rarible.protocol.dto.FlowNftOrderActivitySellDto
import com.rarible.protocol.dto.FlowTransferDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowBurnActivityDto
import com.rarible.protocol.union.dto.FlowMintActivityDto
import com.rarible.protocol.union.dto.FlowOrderActivityMatchSideDto
import com.rarible.protocol.union.dto.FlowOrderCancelListActivityDto
import com.rarible.protocol.union.dto.FlowOrderListActivityDto
import com.rarible.protocol.union.dto.FlowOrderMatchActivityDto
import com.rarible.protocol.union.dto.FlowTransferActivityDto
import com.rarible.protocol.union.dto.UnionActivitiesDto
import com.rarible.protocol.union.dto.UnionActivityDto
import com.rarible.protocol.union.dto.UnionActivityIdDto

object FlowUnionActivityConverter {

    fun convert(source: FlowActivityDto, blockchain: BlockchainDto): UnionActivityDto {
        val unionActivityId = UnionActivityIdDto(blockchain, source.id)
        return when (source) {
            is FlowNftOrderActivitySellDto -> {
                FlowOrderMatchActivityDto(
                    id = unionActivityId,
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
                    id = unionActivityId,
                    date = source.date,
                    price = source.price,
                    priceUsd = source.price, // TODO should be in USD
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(source.maker, blockchain),
                    make = FlowConverter.convert(source.make, blockchain),
                    take = FlowConverter.convert(source.take, blockchain)
                )
            }
            is FlowNftOrderActivityCancelListDto -> {
                FlowOrderCancelListActivityDto(
                    id = unionActivityId,
                    date = source.date,
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(source.maker, blockchain),
                    make = FlowConverter.convert(source.make, blockchain),
                    take = FlowConverter.convert(source.take, blockchain)
                )
            }
            is FlowMintDto -> {
                FlowMintActivityDto(
                    id = unionActivityId,
                    date = source.date,
                    owners = listOf(UnionAddressConverter.convert(source.owner, blockchain)),
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
                    id = unionActivityId,
                    date = source.date,
                    owners = listOf(UnionAddressConverter.convert(source.owner, blockchain)),
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
                    id = unionActivityId,
                    date = source.date,
                    from = UnionAddressConverter.convert(source.from, blockchain),
                    owners = listOf(UnionAddressConverter.convert(source.owner, blockchain)),
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

    fun convert(source: FlowActivitiesDto, blockchain: BlockchainDto): UnionActivitiesDto {
        return UnionActivitiesDto(
            continuation = source.continuation,
            activities = source.items.map { convert(it, blockchain) }
        )
    }

    private fun convert(
        source: com.rarible.protocol.dto.FlowOrderActivityMatchSideDto,
        blockchain: BlockchainDto
    ): FlowOrderActivityMatchSideDto {
        return FlowOrderActivityMatchSideDto(
            maker = UnionAddressConverter.convert(source.maker, blockchain),
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