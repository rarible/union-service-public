package com.rarible.protocol.union.integration.tezos.dipdup.converter

import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupBurnActivity
import com.rarible.dipdup.client.core.model.DipDupMintActivity
import com.rarible.dipdup.client.core.model.DipDupOrderCancelActivity
import com.rarible.dipdup.client.core.model.DipDupOrderListActivity
import com.rarible.dipdup.client.core.model.DipDupOrderSellActivity
import com.rarible.dipdup.client.core.model.DipDupTransferActivity
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.dipdup.client.model.DipDupActivityType
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.converter.UnionConverter
import com.rarible.protocol.union.core.exception.UnionDataFormatException
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderCancelListActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.TransferActivityDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

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

    fun convertToDipDupTypes(source: List<ActivityTypeDto>): List<DipDupActivityType> {
        return source.mapNotNull { convertToDipDupType(it) }
    }

    fun convertToDipDupType(source: ActivityTypeDto) = when (source) {
        ActivityTypeDto.LIST -> DipDupActivityType.LIST
        ActivityTypeDto.SELL -> DipDupActivityType.SELL
        ActivityTypeDto.CANCEL_LIST -> DipDupActivityType.CANCEL_LIST
        else -> null
    }

    fun price(value: BigDecimal, makeValue: BigDecimal, platform: TezosPlatform) =
        when (platform) {
            // remove after ECHO-180
            TezosPlatform.RARIBLE_V2 -> value
            else -> {
                try {
                    value.divide(makeValue, MathContext.DECIMAL128)
                } catch (e: Exception) {
                    logger.error("Failed to calculate price: $value", e)
                    value
                }
            }
        }

    private suspend fun convertInternal(activity: DipDupActivity, blockchain: BlockchainDto): ActivityDto {
        val activityId = ActivityIdDto(blockchain, activity.id)
        val date = activity.date.toInstant()

        return when(activity) {
            is DipDupOrderListActivity -> {
                val make = DipDupConverter.convert(activity.make, blockchain)
                val take = DipDupConverter.convert(activity.take, blockchain)
                val price = price(activity.take.assetValue, activity.make.assetValue, activity.source)

                OrderListActivityDto(
                    id = activityId,
                    date = date,
                    price = price,
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
                val make = DipDupConverter.convert(activity.make.assetType, blockchain)
                val take = DipDupConverter.convert(activity.take.assetType, blockchain)

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
                    price = activity.payment.assetValue,
                    priceUsd = currencyService.toUsd(blockchain, payment.type, payment.value),
                    type = OrderMatchSellDto.Type.SELL
                )
            }
            is DipDupMintActivity -> {
                val id = ActivityIdDto(blockchain, activity.transferId)
                MintActivityDto(
                    id = id,
                    date = date,
                    reverted = activity.reverted,
                    owner = UnionAddressConverter.convert(blockchain, activity.owner),
                    transactionHash = activity.transactionId,
                    value = convertValue(activity.value, id),
                    tokenId = activity.tokenId,
                    itemId = ItemIdDto(blockchain, activity.contract, activity.tokenId),
                    contract = ContractAddress(blockchain, activity.contract)
                )
            }
            is DipDupTransferActivity -> {
                val id = ActivityIdDto(blockchain, activity.transferId)
                TransferActivityDto(
                    id = id,
                    date = date,
                    reverted = activity.reverted,
                    from = UnionAddressConverter.convert(blockchain, activity.from),
                    owner = UnionAddressConverter.convert(blockchain, activity.owner),
                    transactionHash = activity.transactionId,
                    value = convertValue(activity.value, id),
                    tokenId = activity.tokenId,
                    itemId = ItemIdDto(blockchain, activity.contract, activity.tokenId),
                    contract = ContractAddress(blockchain, activity.contract)
                )
            }
            is DipDupBurnActivity -> {
                val id = ActivityIdDto(blockchain, activity.transferId)
                BurnActivityDto(
                    id = id,
                    date = date,
                    reverted = activity.reverted,
                    owner = UnionAddressConverter.convert(blockchain, activity.owner),
                    transactionHash = activity.transactionId,
                    value = convertValue(activity.value, id),
                    tokenId = activity.tokenId,
                    itemId = ItemIdDto(blockchain, activity.contract, activity.tokenId),
                    contract = ContractAddress(blockchain, activity.contract)
                )
            }
        }
    }

    private fun convertValue(bd: BigDecimal, id: ActivityIdDto): BigInteger {
        if (bd.stripTrailingZeros().scale() > 0) {
            logger.warn("Value: $bd must be BigInteger for token activity: $id, tring to multiply by 1_000_000")
            return bd.multiply(BigDecimal(1_000_000)).toBigInteger()
        } else return bd.toBigInteger()
    }

    private fun convert(source: TezosPlatform): OrderActivitySourceDto {
        return when(source) {
            TezosPlatform.Rarible -> OrderActivitySourceDto.RARIBLE
            TezosPlatform.Hen -> OrderActivitySourceDto.HEN
            TezosPlatform.Objkt -> OrderActivitySourceDto.OBJKT
            TezosPlatform.Objkt_v2 -> OrderActivitySourceDto.OBJKT
            TezosPlatform.RARIBLE_V1 -> OrderActivitySourceDto.RARIBLE
            TezosPlatform.RARIBLE_V2 -> OrderActivitySourceDto.RARIBLE
        }
    }
}

