package com.rarible.protocol.union.core.converter.ethereum

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*
import java.time.Instant

object EthUnionOrderEventDtoConverter {

    fun convert(source: OrderEventDto, blockchain: Blockchain): UnionOrderEventDto {
        return when (source) {
            is OrderUpdateEventDto -> {
                EthOrderUpdateEventDto(
                    eventId = source.eventId,
                    orderId = source.orderId,
                    order = when (val order = source.order) {
                        is LegacyOrderDto -> {
                            EthLegacyOrderDto(
                                maker = EthAddressConverter.convert(order.maker),
                                taker = source.order.taker?.let { EthAddressConverter.convert(it) },
                                make = EthAssetDtoConverter.convert(order.make),
                                take = EthAssetDtoConverter.convert(order.take),
                                salt = EthTypesConverter.convert(order.salt),
                                signature = order.signature?.let { EthTypesConverter.convert(it) },
                                pending = order.pending?.map { convert(it) },
                                hash = EthTypesConverter.convert(order.hash),
                                fill = order.fill,
                                startedAt = order.start?.let { Instant.ofEpochSecond(it) },
                                makeStock = order.makeStock.toBigDecimal(), //TODO: Why big decimal
                                cancelled = order.cancelled,
                                createdAt = order.createdAt,
                                lastUpdatedAt = order.lastUpdateAt,
                                makeBalance = order.makeBalance?.toBigDecimal(), //TODO: Need remove
                                makePriceUSD = order.makePriceUsd,
                                takePriceUSD = order.takePriceUsd,
                                priceHistory = order.priceHistory.map { convert(it) },
                                data = EthOrderDataLegacyDto(
                                    fee = order.data.fee.toBigInteger()
                                )
                            )
                        }
                        is RaribleV2OrderDto -> {
                            EthRaribleV2OrderDto(
                                maker = EthAddressConverter.convert(order.maker),
                                taker = source.order.taker?.let { EthAddressConverter.convert(order.maker) },
                                make = EthAssetDtoConverter.convert(order.make),
                                take = EthAssetDtoConverter.convert(order.take),
                                salt = EthTypesConverter.convert(order.salt),
                                signature = order.signature?.let { EthTypesConverter.convert(it) },
                                pending = order.pending?.map { convert(it) },
                                hash = EthTypesConverter.convert(order.hash),
                                fill = order.fill,
                                startedAt = order.start?.let { Instant.ofEpochSecond(it) },
                                makeStock = order.makeStock.toBigDecimal(), //TODO: Why big decimal
                                cancelled = order.cancelled,
                                createdAt = order.createdAt,
                                lastUpdatedAt = order.lastUpdateAt,
                                makeBalance = order.makeBalance?.toBigDecimal(), //TODO: Need remove
                                makePriceUSD = order.makePriceUsd,
                                takePriceUSD = order.takePriceUsd,
                                priceHistory = order.priceHistory.map { convert(it) },
                                data = EthOrderDataRaribleV2DataV1Dto(
                                    payouts = order.data.payouts.map { EthOrderPayoutDtoConverter.convert(it) },
                                    originFees = order.data.originFees.map { EthOrderOriginFeeDtoConverter.convert(it) }
                                )
                            )
                        }
                        is OpenSeaV1OrderDto -> {
                            EthOpenSeaV1OrderDto(
                                maker = EthAddressConverter.convert(order.maker),
                                taker = source.order.taker?.let { EthAddressConverter.convert(order.maker) },
                                make = EthAssetDtoConverter.convert(order.make),
                                take = EthAssetDtoConverter.convert(order.take),
                                salt = EthTypesConverter.convert(order.salt),
                                signature = order.signature?.let { EthTypesConverter.convert(it) },
                                pending = order.pending?.map { convert(it) },
                                hash = EthTypesConverter.convert(order.hash),
                                fill = order.fill,
                                startedAt = order.start?.let { Instant.ofEpochSecond(it) },
                                makeStock = order.makeStock.toBigDecimal(), //TODO: Why big decimal
                                cancelled = order.cancelled,
                                createdAt = order.createdAt,
                                lastUpdatedAt = order.lastUpdateAt,
                                makeBalance = order.makeBalance?.toBigDecimal(), //TODO: Need remove
                                makePriceUSD = order.makePriceUsd,
                                takePriceUSD = order.takePriceUsd,
                                priceHistory = order.priceHistory.map { convert(it) },
                                data = EthOrderOpenSeaV1DataV1Dto(
                                    exchange = EthAddressConverter.convert(order.data.exchange),
                                    makerRelayerFee = order.data.makerRelayerFee,
                                    takerRelayerFee = order.data.takerRelayerFee,
                                    makerProtocolFee = order.data.makerProtocolFee,
                                    takerProtocolFee = order.data.takerProtocolFee,
                                    feeRecipient = EthAddressConverter.convert(order.data.feeRecipient),
                                    feeMethod = convert(order.data.feeMethod),
                                    side = convert(order.data.side),
                                    saleKind = convert(order.data.saleKind),
                                    howToCall = convert(order.data.howToCall),
                                    callData = EthTypesConverter.convert(order.data.callData),
                                    replacementPattern = EthTypesConverter.convert(order.data.replacementPattern),
                                    staticTarget = EthAddressConverter.convert(order.data.staticTarget),
                                    staticExtraData = EthTypesConverter.convert(order.data.staticExtraData),
                                    extra = order.data.extra
                                )
                            )
                        }
                    }
                )
            }
        }
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

    private fun convert(source: OrderExchangeHistoryDto): EthPendingOrderDto {
        return when (source) {
            is OrderSideMatchDto -> EthPendingOrderMatchDto(
                side = source.side?.let { convert(it) },
                fill = source.fill,
                taker = source.taker?.let { EthAddressConverter.convert(it) },
                counterHash = source.counterHash?.let { EthTypesConverter.convert(it) },
                makeUSD = source.makeUsd?.toBigInteger(), //TODO: Need BigDecimal
                takeUSD = source.takeUsd?.toBigInteger(), //TODO: Need BigDecimal
                makePriceUSD = source.takePriceUsd?.toBigInteger(), //TODO: Need BigDecimal
                takePriceUSD = source.makePriceUsd?.toBigInteger() //TODO: Need BigDecimal
            )
            is OrderCancelDto -> EthPendingOrderCancelDto(
                //TODO: Not full object
                owner = source.owner?.let { EthAddressConverter.convert(it) }
            )
        }
    }

    private fun convert(source: OrderPriceHistoryRecordDto): UnionOrderPriceHistoryRecordDto {
        return UnionOrderPriceHistoryRecordDto(
            date = source.date,
            makeValue = source.makeValue,
            takeValue = source.takeValue
        )
    }

    private fun convert(source: OrderSideDto): EthPendingOrderMatchDto.Side {
        return when (source) {
            OrderSideDto.RIGHT -> EthPendingOrderMatchDto.Side.RIGHT //TODO: Maybe need separate type
            OrderSideDto.LEFT -> EthPendingOrderMatchDto.Side.LEFT
        }
    }
}

