package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.dto.FlowOrdersPaginationDto
import com.rarible.protocol.dto.PayInfoDto
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.ext
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowOrderDataV1Dto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderPayoutDto
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
            logger.error("Failed to convert Flow Order, cause: {} \n{}", e.message, order)
            throw e
        }
    }

    private suspend fun convertInternal(order: FlowOrderDto, blockchain: BlockchainDto): OrderDto {

        val make = FlowConverter.convert(order.make, blockchain)
        val take = FlowConverter.convert(order.take, blockchain)

        val maker = UnionAddressConverter.convert(order.maker, blockchain)
        val taker = order.taker?.let { UnionAddressConverter.convert(it, blockchain) }

        val takePrice = order.take.value / order.make.value
        val takePriceUsd = currencyService.toUsd(blockchain, take.type.ext.contract, takePrice)

        return OrderDto(
            id = OrderIdDto(blockchain, order.id.toString()),
            platform = PlatformDto.RARIBLE,
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
            makePrice = null,
            takePrice = takePrice,
            makePriceUsd = null,
            takePriceUsd = takePriceUsd,
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

    private fun convert(
        source: com.rarible.protocol.dto.FlowOrderDataDto,
        blockchain: BlockchainDto
    ): FlowOrderDataV1Dto {
        return FlowOrderDataV1Dto(
            payouts = source.payouts.map { convert(it, blockchain) },
            originFees = source.originalFees.map { convert(it, blockchain) }
        )
    }

    private fun convert(source: PayInfoDto, blockchain: BlockchainDto): OrderPayoutDto {
        return OrderPayoutDto(
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = source.value.toBigInteger()
        )
    }
}

