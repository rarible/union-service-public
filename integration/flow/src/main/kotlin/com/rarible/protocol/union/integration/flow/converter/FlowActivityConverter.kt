package com.rarible.protocol.union.integration.flow.converter

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
import com.rarible.protocol.union.core.service.CurrencyService
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
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class FlowActivityConverter(
    private val currencyService: CurrencyService
) {

    suspend fun convert(source: FlowActivityDto, blockchain: BlockchainDto): ActivityDto {
        val activityId = ActivityIdDto(blockchain, source.id)
        return when (source) {
            is FlowNftOrderActivitySellDto -> {
                val nft = FlowConverter.convert(source.left.asset, blockchain)
                val payment = FlowConverter.convert(source.right.asset, blockchain)
                val priceUsd = currencyService
                    .toUsd(blockchain, payment.type, source.price) ?: BigDecimal.ZERO

                OrderMatchSellDto(
                    id = activityId,
                    date = source.date,
                    nft = nft,
                    payment = payment,
                    seller = UnionAddressConverter.convert(source.left.maker, blockchain),
                    buyer = UnionAddressConverter.convert(source.right.maker, blockchain),
                    // TODO FLOW there is no order info in flow for sides
                    sellerOrderHash = null,
                    buyerOrderHash = null,
                    priceUsd = priceUsd,
                    price = source.price,
                    type = OrderMatchSellDto.Type.SELL,
                    // TODO FLOW here should be price from FLOW, we don't want to calculate it here
                    amountUsd = amountUsd(priceUsd, source.left.asset),
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
                val payment = FlowConverter.convert(source.take, blockchain)
                val priceUsd = currencyService
                    .toUsd(blockchain, payment.type, source.price) ?: BigDecimal.ZERO

                OrderListActivityDto(
                    id = activityId,
                    date = source.date,
                    price = source.price,
                    // TODO FLOW here should be price from FLOW, we don't want to calculate it here
                    priceUsd = priceUsd,
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
                    contract = UnionAddressConverter.convert(source.contract, blockchain),
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
                    contract = UnionAddressConverter.convert(source.contract, blockchain),
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
                    contract = UnionAddressConverter.convert(source.contract, blockchain),
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

    suspend fun convert(source: FlowActivitiesDto, blockchain: BlockchainDto): Slice<ActivityDto> {
        return Slice(
            continuation = source.continuation,
            entities = source.items.map { convert(it, blockchain) }
        )
    }
}