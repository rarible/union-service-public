package com.rarible.protocol.union.integration.tezos.dipdup.converter

import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.client.core.model.OrderStatus
import com.rarible.dipdup.client.model.DipDupOrderSort
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.core.util.evalMakePrice
import com.rarible.protocol.union.core.util.evalTakePrice
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDataDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.TezosOrderDataRaribleV2DataV2Dto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DipDupOrderConverter(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(order: DipDupOrder, blockchain: BlockchainDto): OrderDto {
        try {
            return convertInternal(order, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Order: {} \n{}", blockchain, e.message, order)
            throw e
        }
    }

    suspend fun convert(source: List<Asset.AssetType>, blockchain: BlockchainDto): List<AssetTypeDto> {
        try {
            return source.map { DipDupConverter.convert(it, blockchain) }
        } catch (e: Exception) {
            logger.error("Failed to convert {} list of assets: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    private suspend fun convertInternal(order: DipDupOrder, blockchain: BlockchainDto): OrderDto {

        val make = DipDupConverter.convert(order.make, blockchain)
        val take = DipDupConverter.convert(order.take, blockchain)

        val maker = UnionAddressConverter.convert(blockchain, order.maker)
        val taker = order.taker?.let { UnionAddressConverter.convert(blockchain, it) }

        // For BID (make = currency, take - NFT) we're calculating prices for taker
        val takePrice = evalTakePrice(make, take)
        // For SELL (make = NFT, take - currency) we're calculating prices for maker
        val makePrice = evalMakePrice(make, take)

        // So for USD conversion we are using take.type for MAKE price and vice versa
        val makePriceUsd = currencyService.toUsd(blockchain, take.type, makePrice)
        val takePriceUsd = currencyService.toUsd(blockchain, make.type, takePrice)

        val status = convert(order.status)

        return OrderDto(
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
            makeStock = order.make.assetValue,
            cancelled = order.cancelled,
            createdAt = order.createdAt.toInstant(),
            lastUpdatedAt = order.lastUpdatedAt.toInstant(),
            makePrice = makePrice,
            takePrice = takePrice,
            makePriceUsd = makePriceUsd,
            takePriceUsd = takePriceUsd,
            data = orderData(),
            salt = order.salt.toString(),
            pending = emptyList()
        )
    }

    fun orderData(): OrderDataDto {
        return TezosOrderDataRaribleV2DataV2Dto(
            payouts = listOf(),
            originFees = listOf()
        )
    }

    fun convert(source: OrderStatus): OrderStatusDto {
        return when (source) {
            OrderStatus.ACTIVE -> OrderStatusDto.ACTIVE
            OrderStatus.FILLED -> OrderStatusDto.FILLED
            OrderStatus.CANCELLED -> OrderStatusDto.CANCELLED
            OrderStatus.INACTIVE -> OrderStatusDto.INACTIVE
            OrderStatus.HISTORICAL -> OrderStatusDto.HISTORICAL
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
}
