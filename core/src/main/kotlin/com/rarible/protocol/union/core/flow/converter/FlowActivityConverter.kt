package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowAssetDto
import com.rarible.protocol.dto.FlowBurnDto
import com.rarible.protocol.dto.FlowMintDto
import com.rarible.protocol.dto.FlowNftOrderActivityCancelListDto
import com.rarible.protocol.dto.FlowNftOrderActivityListDto
import com.rarible.protocol.dto.FlowNftOrderActivitySellDto
import com.rarible.protocol.dto.FlowTransferDto
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderCancelListActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.TransferActivityDto
import java.math.BigDecimal

object FlowActivityConverter {

    fun convert(source: FlowActivityDto, blockchain: BlockchainDto): ActivityDto {
        val activityId = ActivityIdDto(blockchain, source.id)
        return when (source) {
            is FlowNftOrderActivitySellDto -> {
                OrderMatchSellDto(
                    id = activityId,
                    // TODO ensure that's right
                    date = source.date,
                    nft = FlowConverter.convert(source.left.asset, blockchain),
                    payment = FlowConverter.convert(source.right.asset, blockchain),
                    seller = UnionAddressConverter.convert(source.left.maker, blockchain),
                    buyer = UnionAddressConverter.convert(source.right.maker, blockchain),
                    priceUsd = source.price, //TODO should be in USD,
                    price = source.price,
                    type = OrderMatchSellDto.Type.SELL,
                    amountUsd = amountUsd(source.price, source.left.asset),
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash,
                        blockHash = source.blockHash,
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    ),
                    source = OrderActivitySourceDto.RARIBLE
                )
            }
            is FlowNftOrderActivityListDto -> {
                OrderListActivityDto(
                    id = activityId,
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
                OrderCancelListActivityDto(
                    id = activityId,
                    date = source.date,
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(source.maker, blockchain),
                    make = FlowConverter.convertToType(source.make, blockchain),
                    take = FlowConverter.convertToType(source.take, blockchain)
                )
            }
            is FlowMintDto -> {
                MintActivityDto(
                    id = activityId,
                    date = source.date,
                    owner = UnionAddressConverter.convert(source.owner, blockchain),
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
                BurnActivityDto(
                    id = activityId,
                    date = source.date,
                    owner = UnionAddressConverter.convert(source.owner, blockchain),
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
                TransferActivityDto(
                    id = activityId,
                    date = source.date,
                    from = UnionAddressConverter.convert(source.from, blockchain),
                    owner = UnionAddressConverter.convert(source.owner, blockchain),
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

    private fun amountUsd(price: BigDecimal, asset: FlowAssetDto) = price.multiply(asset.value)

    fun convert(source: FlowActivitiesDto, blockchain: BlockchainDto): Slice<ActivityDto> {
        return Slice(
            continuation = source.continuation,
            entities = source.items.map { convert(it, blockchain) }
        )
    }
}