package com.rarible.protocol.union.integration.tezos.dipdup.converter

import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupOrderCancelActivity
import com.rarible.dipdup.client.core.model.DipDupOrderListActivity
import com.rarible.dipdup.client.core.model.DipDupOrderSellActivity
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderCancelListActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DipDupActivityConverter(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(source: DipDupActivity, blockchain: BlockchainDto): ActivityDto {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Activity: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    private suspend fun convertInternal(activity: DipDupActivity, blockchain: BlockchainDto): ActivityDto {
        val activityId = ActivityIdDto(blockchain, activity.id)
        val date = activity.date.toInstant()

        return when(activity) {
            is DipDupOrderListActivity -> {
                val make = DipDupConverter.convert(activity.make, blockchain)
                val take = DipDupConverter.convert(activity.take, blockchain)

                OrderListActivityDto(
                    id = activityId,
                    date = date,
                    price = activity.price,
                    priceUsd = currencyService.toUsd(blockchain, take.type, make.value),
                    source = convert(activity.source),
                    hash = activity.hash,
                    maker = UnionAddressConverter.convert(blockchain, activity.maker),
                    make = make,
                    take = take,
                    reverted = activity.reverted
                )
            }
            is DipDupOrderCancelActivity -> {
                val make = DipDupConverter.convert(activity.make.type, blockchain)
                val take = DipDupConverter.convert(activity.take.type, blockchain)

                OrderCancelListActivityDto(
                    id = activityId,
                    date = date,
                    source = convert(activity.source),
                    hash = activity.hash,
                    maker = UnionAddressConverter.convert(blockchain, activity.maker),
                    reverted = false,
                    make = make,
                    take = take,
                    transactionHash = activity.hash
                )
            }
            is DipDupOrderSellActivity -> {
                val nft = DipDupConverter.convert(activity.nft, blockchain)
                val payment = DipDupConverter.convert(activity.payment, blockchain)

                OrderMatchSellDto(
                    id = activityId,
                    date = date,
                    source = convert(activity.source),
                    reverted = false,
                    transactionHash = activity.hash,
                    seller = UnionAddressConverter.convert(blockchain, activity.seller),
                    nft = nft,
                    payment = payment,
                    buyer = UnionAddressConverter.convert(blockchain, activity.buyer),
                    price = activity.price,
                    priceUsd = currencyService.toUsd(blockchain, payment.type, payment.value),
                    type = OrderMatchSellDto.Type.SELL
                )
            }
        }
    }

    private fun convert(source: TezosPlatform): OrderActivitySourceDto {
        return when(source) {
            TezosPlatform.RARIBLE -> OrderActivitySourceDto.RARIBLE
            TezosPlatform.HEN -> OrderActivitySourceDto.RARIBLE // TODO: fix source!
            TezosPlatform.OBJKT -> OrderActivitySourceDto.RARIBLE // TODO: fix source!
            TezosPlatform.OBJKT_V2 -> OrderActivitySourceDto.RARIBLE // TODO: fix source!
        }
    }
}

