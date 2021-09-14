package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*
import java.time.Instant

object EthUnionOrderConverter {

    fun convert(order: OrderDto, blockchain: EthBlockchainDto): EthOrderDto {
        val unionOrderId = EthOrderIdDto(EthConverter.convert(order.hash), blockchain)
        return when (order) {
            is LegacyOrderDto -> {
                EthLegacyOrderDto(
                    id = unionOrderId,
                    maker = EthAddressConverter.convert(order.maker, blockchain),
                    taker = order.taker?.let { EthAddressConverter.convert(it, blockchain) },
                    make = EthConverter.convert(order.make, blockchain),
                    take = EthConverter.convert(order.take, blockchain),
                    salt = EthConverter.convert(order.salt),
                    signature = order.signature?.let { EthConverter.convert(it) },
                    pending = order.pending?.map { convert(it, blockchain) },
                    fill = order.fill,
                    startedAt = order.start?.let { Instant.ofEpochSecond(it) },
                    endedAt = order.end?.let { Instant.ofEpochSecond(it) },
                    makeStock = order.makeStock,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    makePriceUsd = order.makePriceUsd,
                    takePriceUsd = order.takePriceUsd,
                    priceHistory = order.priceHistory?.map { convert(it) } ?: listOf(),
                    data = EthOrderDataLegacyDto(
                        fee = order.data.fee.toBigInteger()
                    )
                )
            }
            is RaribleV2OrderDto -> {
                EthRaribleV2OrderDto(
                    id = unionOrderId,
                    maker = EthAddressConverter.convert(order.maker, blockchain),
                    taker = order.taker?.let { EthAddressConverter.convert(order.taker!!, blockchain) },
                    make = EthConverter.convert(order.make, blockchain),
                    take = EthConverter.convert(order.take, blockchain),
                    salt = EthConverter.convert(order.salt),
                    signature = order.signature?.let { EthConverter.convert(it) },
                    pending = order.pending?.map { convert(it, blockchain) },
                    fill = order.fill,
                    startedAt = order.start?.let { Instant.ofEpochSecond(it) },
                    endedAt = order.end?.let { Instant.ofEpochSecond(it) },
                    makeStock = order.makeStock,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    makePriceUsd = order.makePriceUsd,
                    takePriceUsd = order.takePriceUsd,
                    priceHistory = order.priceHistory?.map { convert(it) } ?: listOf(),
                    data = EthOrderDataRaribleV2DataV1Dto(
                        payouts = order.data.payouts.map { EthConverter.convertToPayout(it, blockchain) },
                        originFees = order.data.originFees.map { EthConverter.convertToPayout(it, blockchain) }
                    )
                )
            }
            is OpenSeaV1OrderDto -> {
                EthOpenSeaV1OrderDto(
                    id = unionOrderId,
                    maker = EthAddressConverter.convert(order.maker, blockchain),
                    taker = order.taker?.let { EthAddressConverter.convert(order.taker!!, blockchain) },
                    make = EthConverter.convert(order.make, blockchain),
                    take = EthConverter.convert(order.take, blockchain),
                    salt = EthConverter.convert(order.salt),
                    signature = order.signature?.let { EthConverter.convert(it) },
                    pending = order.pending?.map { convert(it, blockchain) },
                    fill = order.fill,
                    startedAt = order.start?.let { Instant.ofEpochSecond(it) },
                    endedAt = order.end?.let { Instant.ofEpochSecond(it) },
                    makeStock = order.makeStock,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    makePriceUsd = order.makePriceUsd,
                    takePriceUsd = order.takePriceUsd,
                    priceHistory = order.priceHistory?.map { convert(it) } ?: listOf(),
                    data = EthOrderOpenSeaV1DataV1Dto(
                        exchange = EthAddressConverter.convert(order.data.exchange, blockchain),
                        makerRelayerFee = order.data.makerRelayerFee,
                        takerRelayerFee = order.data.takerRelayerFee,
                        makerProtocolFee = order.data.makerProtocolFee,
                        takerProtocolFee = order.data.takerProtocolFee,
                        feeRecipient = EthAddressConverter.convert(order.data.feeRecipient, blockchain),
                        feeMethod = convert(order.data.feeMethod),
                        side = convert(order.data.side),
                        saleKind = convert(order.data.saleKind),
                        howToCall = convert(order.data.howToCall),
                        callData = EthConverter.convert(order.data.callData),
                        replacementPattern = EthConverter.convert(order.data.replacementPattern),
                        staticTarget = EthAddressConverter.convert(order.data.staticTarget, blockchain),
                        staticExtraData = EthConverter.convert(order.data.staticExtraData),
                        extra = order.data.extra
                    )
                )
            }
            is CryptoPunkOrderDto -> {
                EthCryptoPunksOrderDto(
                    id = unionOrderId,
                    maker = EthAddressConverter.convert(order.maker, blockchain),
                    taker = order.taker?.let { EthAddressConverter.convert(order.taker!!, blockchain) },
                    make = EthConverter.convert(order.make, blockchain),
                    take = EthConverter.convert(order.take, blockchain),
                    salt = EthConverter.convert(order.salt),
                    signature = order.signature?.let { EthConverter.convert(it) },
                    pending = order.pending?.map { convert(it, blockchain) },
                    fill = order.fill,
                    startedAt = order.start?.let { Instant.ofEpochSecond(it) },
                    endedAt = order.end?.let { Instant.ofEpochSecond(it) },
                    makeStock = order.makeStock,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    makePriceUsd = order.makePriceUsd,
                    takePriceUsd = order.takePriceUsd,
                    priceHistory = order.priceHistory?.map { convert(it) } ?: listOf(),
                    data = EthOrderCryptoPunksDataDto()
                )
            }
        }
    }

    fun convert(source: OrdersPaginationDto, blockchain: EthBlockchainDto): UnionOrdersDto {
        return UnionOrdersDto(
            continuation = source.continuation,
            orders = source.orders.map { convert(it, blockchain) }
        )
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


    private fun convert(source: OrderSideDto): EthPendingOrderMatchDto.Side {
        return when (source) {
            OrderSideDto.RIGHT -> EthPendingOrderMatchDto.Side.RIGHT
            OrderSideDto.LEFT -> EthPendingOrderMatchDto.Side.LEFT
        }
    }

    private fun convert(source: OrderPriceHistoryRecordDto): UnionOrderPriceHistoryRecordDto {
        return UnionOrderPriceHistoryRecordDto(
            date = source.date,
            makeValue = source.makeValue,
            takeValue = source.takeValue
        )
    }

    private fun convert(source: OrderExchangeHistoryDto, blockchain: EthBlockchainDto): EthPendingOrderDto {
        return when (source) {
            is OrderSideMatchDto -> EthPendingOrderMatchDto(
                id = EthOrderIdDto(EthConverter.convert(source.hash), blockchain),
                make = source.make?.let { EthConverter.convert(it, blockchain) },
                take = source.take?.let { EthConverter.convert(it, blockchain) },
                date = source.date,
                side = source.side?.let { convert(it) },
                fill = source.fill,
                maker = source.maker?.let { EthAddressConverter.convert(it, blockchain) },
                taker = source.taker?.let { EthAddressConverter.convert(it, blockchain) },
                counterHash = source.counterHash?.let { EthConverter.convert(it) },
                makeUsd = source.makeUsd,
                takeUsd = source.takeUsd,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd
            )
            is OrderCancelDto -> EthPendingOrderCancelDto(
                id = EthOrderIdDto(EthConverter.convert(source.hash), blockchain),
                make = source.make?.let { EthConverter.convert(it, blockchain) },
                take = source.take?.let { EthConverter.convert(it, blockchain) },
                date = source.date,
                maker = source.maker?.let { EthAddressConverter.convert(it, blockchain) },
                owner = source.owner?.let { EthAddressConverter.convert(it, blockchain) }
            )
            is OnChainOrderDto -> EthOnChainOrderDto(
                id = EthOrderIdDto(EthConverter.convert(source.hash), blockchain),
                make = source.make?.let { EthConverter.convert(it, blockchain) },
                take = source.take?.let { EthConverter.convert(it, blockchain) },
                date = source.date,
                maker = source.maker?.let { EthAddressConverter.convert(it, blockchain) }
            )
        }
    }

}

