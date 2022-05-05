package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.tezos.dto.OrderPaginationDto
import com.rarible.protocol.tezos.dto.PartDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.core.util.evalMakePrice
import com.rarible.protocol.union.core.util.evalTakePrice
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PayoutDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.TezosOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.continuation.page.Slice
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TezosOrderConverter(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(order: com.rarible.protocol.tezos.dto.OrderDto, blockchain: BlockchainDto): OrderDto {
        try {
            return convertInternal(order, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Order: {} \n{}", blockchain, e.message, order)
            throw e
        }
    }

    private suspend fun convertInternal(order: com.rarible.protocol.tezos.dto.OrderDto, blockchain: BlockchainDto): OrderDto {

        val make = TezosConverter.convert(order.make, blockchain)
        val take = TezosConverter.convert(order.take, blockchain)

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
            id = OrderIdDto(blockchain, order.hash),
            platform = PlatformDto.RARIBLE,
            maker = maker,
            taker = taker,
            make = make,
            take = take,
            status = status,
            fill = order.fill.toBigDecimal(),
            startedAt = order.start?.let { Instant.ofEpochSecond(it.toLong()) },
            endedAt = order.end?.let { Instant.ofEpochSecond(it.toLong()) },
            makeStock = order.makeStock.toBigDecimal(),
            cancelled = order.cancelled,
            createdAt = order.createdAt,
            lastUpdatedAt = order.lastUpdateAt,
            makePrice = makePrice,
            takePrice = takePrice,
            makePriceUsd = makePriceUsd,
            takePriceUsd = takePriceUsd,
            signature = order.signature,
            data = convertData(order, blockchain),
            salt = order.salt.toString(),
            pending = emptyList() // In Union we won't use this field for Tezos
        )
    }

    suspend fun convert(source: OrderPaginationDto, blockchain: BlockchainDto): Slice<OrderDto> {
        return Slice(
            continuation = source.continuation,
            entities = source.orders.map { convert(it, blockchain) }
        )
    }

    fun convert(source: List<OrderStatusDto>?): List<com.rarible.protocol.tezos.dto.OrderStatusDto>? {
        return source?.map { convert(it) } ?: emptyList()
    }

    fun convert(source: OrderStatusDto): com.rarible.protocol.tezos.dto.OrderStatusDto {
        return when (source) {
            OrderStatusDto.ACTIVE -> com.rarible.protocol.tezos.dto.OrderStatusDto.ACTIVE
            OrderStatusDto.FILLED -> com.rarible.protocol.tezos.dto.OrderStatusDto.FILLED
            OrderStatusDto.HISTORICAL -> com.rarible.protocol.tezos.dto.OrderStatusDto.HISTORICAL
            OrderStatusDto.INACTIVE -> com.rarible.protocol.tezos.dto.OrderStatusDto.INACTIVE
            OrderStatusDto.CANCELLED -> com.rarible.protocol.tezos.dto.OrderStatusDto.CANCELLED
        }
    }

    fun convert(source: com.rarible.protocol.tezos.dto.OrderStatusDto): OrderStatusDto {
        return when (source) {
            com.rarible.protocol.tezos.dto.OrderStatusDto.ACTIVE -> OrderStatusDto.ACTIVE
            com.rarible.protocol.tezos.dto.OrderStatusDto.FILLED -> OrderStatusDto.FILLED
            com.rarible.protocol.tezos.dto.OrderStatusDto.HISTORICAL -> OrderStatusDto.HISTORICAL
            com.rarible.protocol.tezos.dto.OrderStatusDto.INACTIVE -> OrderStatusDto.INACTIVE
            com.rarible.protocol.tezos.dto.OrderStatusDto.CANCELLED -> OrderStatusDto.CANCELLED
        }
    }

    // TODO TEZOS there should be separate enum for Order sorting
    fun convert(source: OrderSortDto?): com.rarible.protocol.tezos.dto.OrderSortDto? {
        return when (source) {
            OrderSortDto.LAST_UPDATE_ASC -> com.rarible.protocol.tezos.dto.OrderSortDto.EARLIEST_FIRST
            OrderSortDto.LAST_UPDATE_DESC -> com.rarible.protocol.tezos.dto.OrderSortDto.LATEST_FIRST
            else -> null
        }
    }

    private fun convertData(
        source: com.rarible.protocol.tezos.dto.OrderDto,
        blockchain: BlockchainDto
    ): TezosOrderDataRaribleV2DataV1Dto {
        return TezosOrderDataRaribleV2DataV1Dto(
            payouts = source.data.payouts.map { convert(it, blockchain) },
            originFees = source.data.originFees.map { convert(it, blockchain) },
            makerEdpk = source.makerEdpk,
            takerEdpk = source.takerEdpk
        )
    }

    private fun convert(source: PartDto, blockchain: BlockchainDto): PayoutDto {
        return PayoutDto(
            account = UnionAddressConverter.convert(blockchain, source.account),
            value = source.value
        )
    }
}

