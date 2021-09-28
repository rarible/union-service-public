package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowBurnDto
import com.rarible.protocol.dto.FlowMintDto
import com.rarible.protocol.dto.FlowNftOrderActivityCancelListDto
import com.rarible.protocol.dto.FlowNftOrderActivityListDto
import com.rarible.protocol.dto.FlowNftOrderActivitySellDto
import com.rarible.protocol.dto.FlowOrderActivityMatchSideDto
import com.rarible.protocol.dto.FlowTransferDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionActivitiesDto
import com.rarible.protocol.union.dto.UnionActivityDto
import com.rarible.protocol.union.dto.UnionActivityIdDto
import com.rarible.protocol.union.dto.UnionBurnActivityDto
import com.rarible.protocol.union.dto.UnionMintActivityDto
import com.rarible.protocol.union.dto.UnionOrderActivityMatchSideDto
import com.rarible.protocol.union.dto.UnionOrderCancelListActivityDto
import com.rarible.protocol.union.dto.UnionOrderListActivityDto
import com.rarible.protocol.union.dto.UnionOrderMatchActivityDto
import com.rarible.protocol.union.dto.UnionTransferActivityDto

object FlowUnionActivityConverter {

    fun convert(source: FlowActivityDto, blockchain: BlockchainDto): UnionActivityDto {
        val unionActivityId = UnionActivityIdDto(blockchain, source.id)
        return when (source) {
            is FlowNftOrderActivitySellDto -> {
                UnionOrderMatchActivityDto(
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
                UnionOrderListActivityDto(
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
                UnionOrderCancelListActivityDto(
                    id = unionActivityId,
                    date = source.date,
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(source.maker, blockchain),
                    make = FlowConverter.convertToType(source.make, blockchain),
                    take = FlowConverter.convertToType(source.take, blockchain)
                )
            }
            is FlowMintDto -> {
                UnionMintActivityDto(
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
                UnionBurnActivityDto(
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
                UnionTransferActivityDto(
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
        source: FlowOrderActivityMatchSideDto,
        blockchain: BlockchainDto
    ): UnionOrderActivityMatchSideDto {
        // TODO как здесь разделять?
        return UnionOrderActivityMatchSideDto(
            maker = UnionAddressConverter.convert(source.maker, blockchain),
            asset = FlowConverter.convert(source.asset, blockchain)
        )
    }
}