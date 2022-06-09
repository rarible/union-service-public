package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.dto.FlowOrderPlatformDto
import com.rarible.protocol.dto.FlowOrderStatusDto
import com.rarible.protocol.dto.FlowOrdersPaginationDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.core.util.evalMakePrice
import com.rarible.protocol.union.core.util.evalTakePrice
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowOrderDataV1Dto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class FlowOrderConverter(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(order: FlowOrderDto, blockchain: BlockchainDto): OrderDto {
        try {
            return convertInternal(order, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Order: {} \n{}", blockchain, e.message, order)
            throw e
        }
    }

    private suspend fun convertInternal(order: FlowOrderDto, blockchain: BlockchainDto): OrderDto {

        val make = FlowConverter.convert(order.make, blockchain)
        val take = FlowConverter.convert(order.take, blockchain)

        val maker = UnionAddressConverter.convert(blockchain, order.maker)
        val taker = order.taker?.let { UnionAddressConverter.convert(blockchain, it) }

        // For BID (make = currency, take - NFT) we're calculating prices for taker
        val takePrice = evalTakePrice(make, take)
        // For SELL (make = NFT, take - currency) we're calculating prices for maker
        val makePrice = evalMakePrice(make, take)
        // So for USD conversion we are using take.type for MAKE price and vice versa
        val makePriceUsd = currencyService.toUsd(blockchain, take.type, makePrice)
        val takePriceUsd = currencyService.toUsd(blockchain, make.type, takePrice)

        val status = convert(order.status!!)

        return OrderDto(
            id = OrderIdDto(blockchain, order.id.toString()),
            platform = when(order.platform) {
                FlowOrderPlatformDto.OTHER -> PlatformDto.OTHER
                else -> PlatformDto.RARIBLE
            },
            status = status,
            maker = maker,
            taker = taker,
            make = make,
            take = take,
            fill = order.fill,
            startedAt = order.start,
            endedAt = order.end,
            makeStock = makeStock(order.makeStock.toBigDecimal()),
            cancelled = order.cancelled,
            createdAt = order.createdAt,
            lastUpdatedAt = order.lastUpdateAt,
            makePrice = makePrice,
            takePrice = takePrice,
            makePriceUsd = makePriceUsd,
            takePriceUsd = takePriceUsd,
            data = convert(order.data, blockchain),
            salt = "",// Not supported on Flow
            dbUpdatedAt = order.lastUpdateAt // TODO change to dbUPdatedAt after Flow Api fix
        )
    }

    private fun makeStock(intVal: BigDecimal): BigDecimal {
        return intVal.movePointLeft(18).stripTrailingZeros() //convert to regular decimal value
    }

    suspend fun convert(order: FlowOrdersPaginationDto, blockchain: BlockchainDto): Slice<OrderDto> {
        return Slice(
            entities = order.items.map { this.convert(it, blockchain) },
            continuation = order.continuation
        )
    }

    fun convert(source: List<OrderStatusDto>?): List<FlowOrderStatusDto>? {
        return source?.map { convert(it) } ?: emptyList()
    }

    fun convert(source: OrderStatusDto): FlowOrderStatusDto {
        return when (source) {
            OrderStatusDto.ACTIVE -> FlowOrderStatusDto.ACTIVE
            OrderStatusDto.FILLED -> FlowOrderStatusDto.FILLED
            OrderStatusDto.HISTORICAL -> FlowOrderStatusDto.HISTORICAL
            OrderStatusDto.INACTIVE -> FlowOrderStatusDto.INACTIVE
            OrderStatusDto.CANCELLED -> FlowOrderStatusDto.CANCELLED
        }
    }

    fun convert(source: FlowOrderStatusDto): OrderStatusDto {
        return when (source) {
            FlowOrderStatusDto.ACTIVE -> OrderStatusDto.ACTIVE
            FlowOrderStatusDto.FILLED -> OrderStatusDto.FILLED
            FlowOrderStatusDto.HISTORICAL -> OrderStatusDto.HISTORICAL
            FlowOrderStatusDto.INACTIVE -> OrderStatusDto.INACTIVE
            FlowOrderStatusDto.CANCELLED -> OrderStatusDto.CANCELLED
        }
    }

    private fun convert(
        source: com.rarible.protocol.dto.FlowOrderDataDto,
        blockchain: BlockchainDto
    ): FlowOrderDataV1Dto {
        return FlowOrderDataV1Dto(
            payouts = source.payouts.map { FlowConverter.convertToPayout(it, blockchain) },
            originFees = source.originalFees.map { FlowConverter.convertToPayout(it, blockchain) }
        )
    }

    fun convert(source: OrderSortDto?): String? {
        return when (source) {
            OrderSortDto.LAST_UPDATE_ASC -> Sort.EARLIEST_FIRST.name
            OrderSortDto.LAST_UPDATE_DESC -> Sort.LATEST_FIRST.name
            else -> null
        }
    }

    enum class Sort {
        EARLIEST_FIRST, LATEST_FIRST
    }

    fun convert(source: SyncSortDto?): String? =
        when (source) {
            SyncSortDto.DB_UPDATE_ASC -> "UPDATED_AT_ASC"
            SyncSortDto.DB_UPDATE_DESC -> "UPDATED_AT_DESC"
            else -> null
        }
}

