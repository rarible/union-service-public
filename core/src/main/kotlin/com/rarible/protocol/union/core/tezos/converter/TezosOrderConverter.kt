package com.rarible.protocol.union.core.tezos.converter

import com.rarible.protocol.tezos.dto.OrderPaginationDto
import com.rarible.protocol.tezos.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.tezos.dto.PartDto
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.ext
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderPayoutDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.TezosOrderDataRaribleV2DataV1Dto
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
            logger.error("Failed to convert Tezos Order, cause: {} \n{}", e.message, order)
            throw e
        }
    }

    private suspend fun convertInternal(order: com.rarible.protocol.tezos.dto.OrderDto, blockchain: BlockchainDto): OrderDto {

        val make = TezosAssetConverter.convert(order.make, blockchain)
        val take = TezosAssetConverter.convert(order.take, blockchain)

        val maker = UnionAddressConverter.convert(order.maker, blockchain)
        val taker = order.taker?.let { UnionAddressConverter.convert(it, blockchain) }

        //todo toBigDecimal???
        val takePrice = order.take.value.toBigDecimal() / order.make.value.toBigDecimal()
        val takePriceUsd = currencyService.toUsd(blockchain, take.type.ext.contract, takePrice)

        return OrderDto(
            id = OrderIdDto(blockchain, order.hash),
            platform = PlatformDto.RARIBLE,
            maker = maker,
            taker = taker,
            make = make,
            take = take,
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
            priceHistory = emptyList(),
            data = convert(order.data, blockchain),
            salt = order.salt
        )
    }

    suspend fun convert(source: OrderPaginationDto, blockchain: BlockchainDto): Slice<OrderDto> {
        return Slice(
            continuation = source.contination,
            entities = source.orders.map { convert(it, blockchain) }
        )
    }

    private fun convert(
        source: OrderRaribleV2DataV1Dto,
        blockchain: BlockchainDto
    ): TezosOrderDataRaribleV2DataV1Dto {
        return TezosOrderDataRaribleV2DataV1Dto(
            payouts = source.payouts.map { convert(it, blockchain) },
            originFees = source.originFees.map { convert(it, blockchain) }
        )
    }

    private fun convert(source: PartDto, blockchain: BlockchainDto): OrderPayoutDto {
        return OrderPayoutDto(
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = source.value //todo why is biginteger here? it's part as in Creator and Royalty
        )
    }
}

