package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.CryptoPunkOrderDto
import com.rarible.protocol.dto.LegacyOrderDto
import com.rarible.protocol.dto.OpenSeaV1OrderDto
import com.rarible.protocol.dto.OrderCancelDto
import com.rarible.protocol.dto.OrderExchangeHistoryDto
import com.rarible.protocol.dto.OrderOpenSeaV1DataV1Dto
import com.rarible.protocol.dto.OrderSideDto
import com.rarible.protocol.dto.OrderSideMatchDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.dto.RaribleV2OrderDto
import com.rarible.protocol.union.core.continuation.Slice
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.ext
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthOrderCryptoPunksDataDto
import com.rarible.protocol.union.dto.EthOrderDataLegacyDto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.EthOrderOpenSeaV1DataV1Dto
import com.rarible.protocol.union.dto.OnChainOrderDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderPriceHistoryRecordDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PendingOrderCancelDto
import com.rarible.protocol.union.dto.PendingOrderDto
import com.rarible.protocol.union.dto.PendingOrderMatchDto
import com.rarible.protocol.union.dto.PlatformDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class EthOrderConverter(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(order: com.rarible.protocol.dto.OrderDto, blockchain: BlockchainDto): OrderDto {
        try {
            return convertInternal(order, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert Ethereum Order, cause {}: \n{}", e.message, order)
            throw e
        }
    }

    private suspend fun convertInternal(order: com.rarible.protocol.dto.OrderDto, blockchain: BlockchainDto): OrderDto {
        val orderId = OrderIdDto(blockchain, EthConverter.convert(order.hash))
        val maker = UnionAddressConverter.convert(order.maker, blockchain)
        val taker = order.taker?.let { UnionAddressConverter.convert(it, blockchain) }
        val make = EthConverter.convert(order.make, blockchain)
        val take = EthConverter.convert(order.take, blockchain)
        val salt = EthConverter.convert(order.salt)
        val startedAt = order.start?.let { Instant.ofEpochSecond(it) }
        val endedAt = order.end?.let { Instant.ofEpochSecond(it) }
        val makePriceUsd = currencyService.toUsd(blockchain, make.type.ext.contract, order.makePrice)
        val takePriceUsd = currencyService.toUsd(blockchain, take.type.ext.contract, order.takePrice)
        val signature = order.signature?.let { EthConverter.convert(it) }
        val pending = order.pending?.map { convert(it, blockchain) }
        val priceHistory = order.priceHistory?.map { convert(it) } ?: listOf()
        return when (order) {
            is LegacyOrderDto -> {
                OrderDto(
                    id = orderId,
                    platform = PlatformDto.RARIBLE,
                    maker = maker,
                    taker = taker,
                    make = make,
                    take = take,
                    salt = salt,
                    signature = signature,
                    pending = pending,
                    fill = order.fillValue!!,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    makeStock = order.makeStockValue!!,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    makePrice = order.makePrice,
                    takePrice = order.takePrice,
                    makePriceUsd = makePriceUsd,
                    takePriceUsd = takePriceUsd,
                    priceHistory = priceHistory,
                    data = EthOrderDataLegacyDto(
                        fee = order.data.fee.toBigInteger()
                    )
                )
            }
            is RaribleV2OrderDto -> {
                OrderDto(
                    id = orderId,
                    platform = PlatformDto.RARIBLE,
                    maker = maker,
                    taker = taker,
                    make = make,
                    take = take,
                    salt = salt,
                    signature = signature,
                    pending = pending,
                    fill = order.fillValue!!,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    makeStock = order.makeStockValue!!,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    makePrice = order.makePrice,
                    takePrice = order.takePrice,
                    makePriceUsd = makePriceUsd,
                    takePriceUsd = takePriceUsd,
                    priceHistory = priceHistory,
                    data = EthOrderDataRaribleV2DataV1Dto(
                        payouts = order.data.payouts.map { EthConverter.convertToPayout(it, blockchain) },
                        originFees = order.data.originFees.map { EthConverter.convertToPayout(it, blockchain) }
                    )
                )
            }
            is OpenSeaV1OrderDto -> {
                OrderDto(
                    id = orderId,
                    platform = PlatformDto.OPEN_SEA,
                    maker = maker,
                    taker = taker,
                    make = make,
                    take = take,
                    salt = salt,
                    signature = signature,
                    pending = pending,
                    fill = order.fillValue!!,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    makeStock = order.makeStockValue!!,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    makePrice = order.makePrice,
                    takePrice = order.takePrice,
                    makePriceUsd = makePriceUsd,
                    takePriceUsd = takePriceUsd,
                    priceHistory = priceHistory,
                    data = EthOrderOpenSeaV1DataV1Dto(
                        exchange = UnionAddressConverter.convert(order.data.exchange, blockchain),
                        makerRelayerFee = order.data.makerRelayerFee,
                        takerRelayerFee = order.data.takerRelayerFee,
                        makerProtocolFee = order.data.makerProtocolFee,
                        takerProtocolFee = order.data.takerProtocolFee,
                        feeRecipient = UnionAddressConverter.convert(order.data.feeRecipient, blockchain),
                        feeMethod = convert(order.data.feeMethod),
                        side = convert(order.data.side),
                        saleKind = convert(order.data.saleKind),
                        howToCall = convert(order.data.howToCall),
                        callData = EthConverter.convert(order.data.callData),
                        replacementPattern = EthConverter.convert(order.data.replacementPattern),
                        staticTarget = UnionAddressConverter.convert(order.data.staticTarget, blockchain),
                        staticExtraData = EthConverter.convert(order.data.staticExtraData),
                        extra = order.data.extra
                    )
                )
            }
            is CryptoPunkOrderDto -> {
                OrderDto(
                    id = orderId,
                    platform = PlatformDto.CRYPTO_PUNKS,
                    maker = maker,
                    taker = taker,
                    make = make,
                    take = take,
                    salt = salt,
                    signature = signature,
                    pending = pending,
                    fill = order.fillValue!!,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    makeStock = order.makeStockValue!!,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    makePrice = order.makePrice,
                    takePrice = order.takePrice,
                    makePriceUsd = makePriceUsd,
                    takePriceUsd = takePriceUsd,
                    priceHistory = priceHistory,
                    data = EthOrderCryptoPunksDataDto()
                )
            }
        }
    }

    suspend fun convert(source: OrdersPaginationDto, blockchain: BlockchainDto): Slice<OrderDto> {
        return Slice(
            continuation = source.continuation,
            entities = source.orders.map { convert(it, blockchain) }
        )
    }

    fun convert(source: OrderStatusDto): com.rarible.protocol.dto.OrderStatusDto {
        return when (source) {
            OrderStatusDto.ACTIVE -> com.rarible.protocol.dto.OrderStatusDto.ACTIVE
            OrderStatusDto.FILLED -> com.rarible.protocol.dto.OrderStatusDto.FILLED
            OrderStatusDto.HISTORICAL -> com.rarible.protocol.dto.OrderStatusDto.HISTORICAL
            OrderStatusDto.INACTIVE -> com.rarible.protocol.dto.OrderStatusDto.INACTIVE
            OrderStatusDto.CANCELLED -> com.rarible.protocol.dto.OrderStatusDto.CANCELLED
        }
    }

    fun convert(source: List<OrderStatusDto>?): List<com.rarible.protocol.dto.OrderStatusDto>? {
        return source?.map { convert(it) }
    }

    private fun convert(source: OrderOpenSeaV1DataV1Dto.FeeMethod): EthOrderOpenSeaV1DataV1Dto.FeeMethod {
        return when (source) {
            OrderOpenSeaV1DataV1Dto.FeeMethod.PROTOCOL_FEE -> EthOrderOpenSeaV1DataV1Dto.FeeMethod.PROTOCOL_FEE
            OrderOpenSeaV1DataV1Dto.FeeMethod.SPLIT_FEE -> EthOrderOpenSeaV1DataV1Dto.FeeMethod.SPLIT_FEE
        }
    }

    private fun convert(source: OrderOpenSeaV1DataV1Dto.Side): EthOrderOpenSeaV1DataV1Dto.Side {
        return when (source) {
            OrderOpenSeaV1DataV1Dto.Side.SELL -> EthOrderOpenSeaV1DataV1Dto.Side.SELL
            OrderOpenSeaV1DataV1Dto.Side.BUY -> EthOrderOpenSeaV1DataV1Dto.Side.BUY
        }
    }

    private fun convert(source: OrderOpenSeaV1DataV1Dto.SaleKind): EthOrderOpenSeaV1DataV1Dto.SaleKind {
        return when (source) {
            OrderOpenSeaV1DataV1Dto.SaleKind.FIXED_PRICE -> EthOrderOpenSeaV1DataV1Dto.SaleKind.FIXED_PRICE
            OrderOpenSeaV1DataV1Dto.SaleKind.DUTCH_AUCTION -> EthOrderOpenSeaV1DataV1Dto.SaleKind.DUTCH_AUCTION
        }
    }

    private fun convert(source: OrderOpenSeaV1DataV1Dto.HowToCall): EthOrderOpenSeaV1DataV1Dto.HowToCall {
        return when (source) {
            OrderOpenSeaV1DataV1Dto.HowToCall.CALL -> EthOrderOpenSeaV1DataV1Dto.HowToCall.CALL
            OrderOpenSeaV1DataV1Dto.HowToCall.DELEGATE_CALL -> EthOrderOpenSeaV1DataV1Dto.HowToCall.DELEGATE_CALL
        }
    }


    private fun convert(source: OrderSideDto): PendingOrderMatchDto.Side {
        return when (source) {
            OrderSideDto.RIGHT -> PendingOrderMatchDto.Side.RIGHT
            OrderSideDto.LEFT -> PendingOrderMatchDto.Side.LEFT
        }
    }

    private fun convert(source: com.rarible.protocol.dto.OrderPriceHistoryRecordDto): OrderPriceHistoryRecordDto {
        return OrderPriceHistoryRecordDto(
            date = source.date,
            makeValue = source.makeValue,
            takeValue = source.takeValue
        )
    }

    private fun convert(source: OrderExchangeHistoryDto, blockchain: BlockchainDto): PendingOrderDto {
        return when (source) {
            is OrderSideMatchDto -> PendingOrderMatchDto(
                id = OrderIdDto(blockchain, EthConverter.convert(source.hash)),
                make = source.make?.let { EthConverter.convert(it, blockchain) },
                take = source.take?.let { EthConverter.convert(it, blockchain) },
                date = source.date,
                side = source.side?.let { convert(it) },
                /** TODO [OrderSideMatchDto.fill] must be BigDecimal, or fillValue */
                fill = source.fill.toBigDecimal(),
                maker = source.maker?.let { UnionAddressConverter.convert(it, blockchain) },
                taker = source.taker?.let { UnionAddressConverter.convert(it, blockchain) },
                counterHash = source.counterHash?.let { EthConverter.convert(it) },
                makeUsd = source.makeUsd,
                takeUsd = source.takeUsd,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd
            )
            is OrderCancelDto -> PendingOrderCancelDto(
                id = OrderIdDto(blockchain, EthConverter.convert(source.hash)),
                make = source.make?.let { EthConverter.convert(it, blockchain) },
                take = source.take?.let { EthConverter.convert(it, blockchain) },
                date = source.date,
                maker = source.maker?.let { UnionAddressConverter.convert(it, blockchain) },
                owner = source.owner?.let { UnionAddressConverter.convert(it, blockchain) }
            )
            is com.rarible.protocol.dto.OnChainOrderDto -> OnChainOrderDto(
                id = OrderIdDto(blockchain, EthConverter.convert(source.hash)),
                make = source.make?.let { EthConverter.convert(it, blockchain) },
                take = source.take?.let { EthConverter.convert(it, blockchain) },
                date = source.date,
                maker = source.maker?.let { UnionAddressConverter.convert(it, blockchain) }
            )
        }
    }

}

