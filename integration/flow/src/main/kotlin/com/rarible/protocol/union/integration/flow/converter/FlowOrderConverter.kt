package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.dto.FlowOrderStatusDto
import com.rarible.protocol.dto.FlowOrdersPaginationDto
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.core.util.evalMakePrice
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowOrderDataV1Dto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

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

        val makePrice = evalMakePrice(make, take)
        val makePriceUsd = currencyService.toUsd(blockchain, take.type, makePrice)

        val status = convert(order.status!!)

        return OrderDto(
            id = OrderIdDto(blockchain, order.id.toString()),
            platform = PlatformDto.RARIBLE,
            status = status,
            maker = maker,
            taker = taker,
            make = make,
            take = take,
            fill = order.fill,
            startedAt = order.start,
            endedAt = order.end,
            makeStock = order.makeStock.toBigDecimal(),
            cancelled = order.cancelled,
            createdAt = order.createdAt,
            lastUpdatedAt = order.lastUpdateAt,
            makePrice = makePrice,
            takePrice = null,
            makePriceUsd = makePriceUsd,
            takePriceUsd = null,
            priceHistory = emptyList(),
            data = convert(order.data, blockchain),
            salt = ""// Not supported on Flow
        )
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
}

