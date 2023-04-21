package com.rarible.protocol.union.integration.tezos.dipdup.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.client.core.model.OrderStatus
import com.rarible.dipdup.client.core.model.Part
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.dipdup.client.model.DipDupOrderSort
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDataDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PayoutDto
import com.rarible.protocol.union.dto.TezosOrderDataFxhashV1Dto
import com.rarible.protocol.union.dto.TezosOrderDataFxhashV2Dto
import com.rarible.protocol.union.dto.TezosOrderDataHenDto
import com.rarible.protocol.union.dto.TezosOrderDataLegacyDto
import com.rarible.protocol.union.dto.TezosOrderDataObjktV1Dto
import com.rarible.protocol.union.dto.TezosOrderDataObjktV2Dto
import com.rarible.protocol.union.dto.TezosOrderDataRaribleV2DataV2Dto
import com.rarible.protocol.union.dto.TezosOrderDataTeiaV1Dto
import com.rarible.protocol.union.dto.TezosOrderDataVersumV1Dto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class DipDupOrderConverter(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val mapper = ObjectMapper()

    suspend fun convert(order: DipDupOrder, blockchain: BlockchainDto): UnionOrder {
        try {
            return convertInternal(order, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Order: {} \n{}", blockchain, e.message, order)
            throw e
        }
    }

    suspend fun convert(source: List<Asset.AssetType>, blockchain: BlockchainDto): List<UnionAssetType> {
        try {
            return source.map { DipDupConverter.convert(it, blockchain) }
        } catch (e: Exception) {
            logger.error("Failed to convert {} list of assets: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    private suspend fun convertInternal(order: DipDupOrder, blockchain: BlockchainDto): UnionOrder {

        val make = DipDupConverter.convert(order.make, blockchain)
        val take = DipDupConverter.convert(order.take, blockchain)

        val maker = UnionAddressConverter.convert(blockchain, order.maker)
        val taker = order.taker?.let { UnionAddressConverter.convert(blockchain, it) }

        // For BID (make = currency, take - NFT) we're calculating prices for taker
        val takePrice = evalTakePrice(make, take, order.takePrice)
        // For SELL (make = NFT, take - currency) we're calculating prices for maker
        val makePrice = evalMakePrice(make, take, order.makePrice)

        // So for USD conversion we are using take.type for MAKE price and vice versa
        val makePriceUsd = currencyService.toUsd(blockchain, take.type, makePrice)
        val takePriceUsd = currencyService.toUsd(blockchain, make.type, takePrice)

        val status = convert(order.status)

        return UnionOrder(
            id = OrderIdDto(blockchain, order.id),
            platform = DipDupConverter.convert(order.platform),
            maker = maker,
            taker = taker,
            make = make,
            take = take,
            status = status,
            fill = order.fill,
            startedAt = order.startAt?.toInstant(),
            endedAt = order.endAt?.toInstant(),
            makeStock = makeStock(order.make, order.fill),
            cancelled = order.cancelled,
            createdAt = order.createdAt.toInstant(),
            lastUpdatedAt = order.lastUpdatedAt.toInstant(),
            makePrice = makePrice,
            takePrice = takePrice,
            makePriceUsd = makePriceUsd,
            takePriceUsd = takePriceUsd,
            data = orderData(order, blockchain),
            salt = order.salt.toString(),
            pending = emptyList()
        )
    }

    fun makeStock(asset: Asset, fill: BigDecimal): BigDecimal {
        return asset.assetValue - fill
    }

    fun orderData(order: DipDupOrder, blockchain: BlockchainDto): OrderDataDto {
        return when (order.platform) {
            TezosPlatform.RARIBLE_V1 -> TezosOrderDataLegacyDto(
                payouts = order.payouts.map { convert(it, blockchain) },
                originFees = order.originFees.map { convert(it, blockchain) },
                legacyData = order.legacyData?.let { mapper.writeValueAsString(it) }
            )
            TezosPlatform.RARIBLE_V2 -> TezosOrderDataRaribleV2DataV2Dto(
                payouts = order.payouts.map { convert(it, blockchain) },
                originFees = order.originFees.map { convert(it, blockchain) }
            )
            TezosPlatform.HEN -> TezosOrderDataHenDto(
                internalOrderId = order.internalOrderId,
                payouts = order.payouts.map { convert(it, blockchain) },
                originFees = order.originFees.map { convert(it, blockchain) }
            )
            TezosPlatform.VERSUM_V1 -> TezosOrderDataVersumV1Dto(
                internalOrderId = order.internalOrderId,
                payouts = order.payouts.map { convert(it, blockchain) },
                originFees = order.originFees.map { convert(it, blockchain) }
            )
            TezosPlatform.TEIA_V1 -> TezosOrderDataTeiaV1Dto(
                internalOrderId = order.internalOrderId,
                payouts = order.payouts.map { convert(it, blockchain) },
                originFees = order.originFees.map { convert(it, blockchain) }
            )
            TezosPlatform.OBJKT_V1 -> TezosOrderDataObjktV1Dto(
                internalOrderId = order.internalOrderId,
                payouts = order.payouts.map { convert(it, blockchain) },
                originFees = order.originFees.map { convert(it, blockchain) }
            )
            TezosPlatform.OBJKT_V2 -> TezosOrderDataObjktV2Dto(
                internalOrderId = order.internalOrderId,
                payouts = order.payouts.map { convert(it, blockchain) },
                originFees = order.originFees.map { convert(it, blockchain) }
            )
            TezosPlatform.FXHASH_V1 -> TezosOrderDataFxhashV1Dto(
                internalOrderId = order.internalOrderId,
                payouts = order.payouts.map { convert(it, blockchain) },
                originFees = order.originFees.map { convert(it, blockchain) }
            )
            TezosPlatform.FXHASH_V2 -> TezosOrderDataFxhashV2Dto(
                internalOrderId = order.internalOrderId,
                payouts = order.payouts.map { convert(it, blockchain) },
                originFees = order.originFees.map { convert(it, blockchain) }
            )
        }
    }

    private fun convert(source: Part, blockchain: BlockchainDto): PayoutDto {
        return PayoutDto(
            account = UnionAddressConverter.convert(blockchain, source.account),
            value = source.value
        )
    }

    fun convert(source: OrderStatus): UnionOrder.Status {
        return when (source) {
            OrderStatus.ACTIVE -> UnionOrder.Status.ACTIVE
            OrderStatus.FILLED -> UnionOrder.Status.FILLED
            OrderStatus.CANCELLED -> UnionOrder.Status.CANCELLED
            OrderStatus.INACTIVE -> UnionOrder.Status.INACTIVE
            OrderStatus.HISTORICAL -> UnionOrder.Status.HISTORICAL
        }
    }

    fun convert(source: OrderStatusDto): OrderStatus {
        return when (source) {
            OrderStatusDto.ACTIVE -> OrderStatus.ACTIVE
            OrderStatusDto.FILLED -> OrderStatus.FILLED
            OrderStatusDto.CANCELLED -> OrderStatus.CANCELLED
            OrderStatusDto.INACTIVE -> OrderStatus.INACTIVE
            OrderStatusDto.HISTORICAL -> OrderStatus.HISTORICAL
        }
    }

    fun convert(source: OrderSortDto) = when (source) {
        OrderSortDto.LAST_UPDATE_ASC -> DipDupOrderSort.LAST_UPDATE_ASC
        OrderSortDto.LAST_UPDATE_DESC -> DipDupOrderSort.LAST_UPDATE_DESC
    }

    fun evalMakePrice(make: UnionAsset, take: UnionAsset, currentPrice: BigDecimal?): BigDecimal? {
        return if (make.type.isNft()) currentPrice ?: (take.value.setScale(18) / make.value) else null
    }

    fun evalTakePrice(make: UnionAsset, take: UnionAsset, currentPrice: BigDecimal?): BigDecimal? {
        return if (take.type.isNft()) currentPrice ?: (make.value.setScale(18) / take.value) else null
    }
}
