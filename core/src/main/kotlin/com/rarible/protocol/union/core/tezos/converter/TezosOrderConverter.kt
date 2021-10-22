package com.rarible.protocol.union.core.tezos.converter

import com.rarible.protocol.tezos.dto.OrderPaginationDto
import com.rarible.protocol.tezos.dto.PartDto
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.ext
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderPayoutDto
import com.rarible.protocol.union.dto.OrderPriceHistoryRecordDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.TezosOrderDataRaribleV2DataV1Dto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.BigInteger
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
            logger.error("Failed to convert Tezos Order, cause: {} \n{}", e.message, order)
            throw e
        }
    }

    private suspend fun convertInternal(order: com.rarible.protocol.tezos.dto.OrderDto, blockchain: BlockchainDto): OrderDto {

        val make = TezosConverter.convert(order.make, blockchain)
        val take = TezosConverter.convert(order.take, blockchain)

        val maker = UnionAddressConverter.convert(order.maker, blockchain)
        val taker = order.taker?.let { UnionAddressConverter.convert(it, blockchain) }

        val takePrice = order.take.value / order.make.value
        val takePriceUsd = currencyService.toUsd(blockchain, take.type.ext.contract, takePrice)

        //TODO FLOW That's not correct! Just a stub until Flow starts to return status
        val status = calculateStatus(order.fill.toBigDecimal(), take, order.makeStock, order.cancelled)

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
            makePrice = null,
            takePrice = takePrice,
            makePriceUsd = null,
            takePriceUsd = takePriceUsd,
            signature = order.signature,
            priceHistory = order.priceHistory?.map { convert(it) } ?: listOf(),
            data = convertData(order, blockchain),
            salt = order.salt,
            pending = emptyList() // TODO TEZOS in union we won't use this field
        )
    }

    suspend fun convert(source: OrderPaginationDto, blockchain: BlockchainDto): Slice<OrderDto> {
        return Slice(
            continuation = source.contination,
            entities = source.orders.map { convert(it, blockchain) }
        )
    }

    private fun convert(source: com.rarible.protocol.tezos.dto.OrderPriceHistoryRecordDto): OrderPriceHistoryRecordDto {
        return OrderPriceHistoryRecordDto(
            date = source.date,
            makeValue = source.makeValue,
            takeValue = source.takeValue
        )
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

    private fun convert(source: PartDto, blockchain: BlockchainDto): OrderPayoutDto {
        return OrderPayoutDto(
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = source.value.toBigInteger() //TODO UNION why BigInteger?
        )
    }

    // TODO TEZOS remove later
    private fun calculateStatus(
        fill: BigDecimal,
        take: AssetDto,
        makeStock: BigInteger,
        cancelled: Boolean
    ): com.rarible.protocol.union.dto.OrderStatusDto {
        return when {
            fill == take.value -> com.rarible.protocol.union.dto.OrderStatusDto.FILLED
            makeStock > BigInteger.ZERO -> com.rarible.protocol.union.dto.OrderStatusDto.ACTIVE
            cancelled -> com.rarible.protocol.union.dto.OrderStatusDto.CANCELLED
            else -> com.rarible.protocol.union.dto.OrderStatusDto.INACTIVE
        }
    }
}

