package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.*
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.*
import java.time.Instant

object EthUnionOrderConverter {

    fun convert(order: OrderDto, blockchain: BlockchainDto): UnionOrderDto {
        val unionOrderId = UnionOrderIdDto(blockchain, EthConverter.convert(order.hash))
        return when (order) {
            is LegacyOrderDto -> {
                UnionOrderDto(
                    id = unionOrderId,
                    maker = UnionAddressConverter.convert(order.maker, blockchain),
                    taker = order.taker?.let { UnionAddressConverter.convert(it, blockchain) },
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
                UnionOrderDto(
                    id = unionOrderId,
                    maker = UnionAddressConverter.convert(order.maker, blockchain),
                    taker = order.taker?.let { UnionAddressConverter.convert(order.taker!!, blockchain) },
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
                UnionOrderDto(
                    id = unionOrderId,
                    maker = UnionAddressConverter.convert(order.maker, blockchain),
                    taker = order.taker?.let { UnionAddressConverter.convert(order.taker!!, blockchain) },
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
                UnionOrderDto(
                    id = unionOrderId,
                    maker = UnionAddressConverter.convert(order.maker, blockchain),
                    taker = order.taker?.let { UnionAddressConverter.convert(order.taker!!, blockchain) },
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

    fun convert(source: OrdersPaginationDto, blockchain: BlockchainDto): UnionOrdersDto {
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


    private fun convert(source: OrderSideDto): UnionPendingOrderMatchDto.Side {
        return when (source) {
            OrderSideDto.RIGHT -> UnionPendingOrderMatchDto.Side.RIGHT
            OrderSideDto.LEFT -> UnionPendingOrderMatchDto.Side.LEFT
        }
    }

    private fun convert(source: OrderPriceHistoryRecordDto): UnionOrderPriceHistoryRecordDto {
        return UnionOrderPriceHistoryRecordDto(
            date = source.date,
            makeValue = source.makeValue,
            takeValue = source.takeValue
        )
    }

    private fun convert(source: OrderExchangeHistoryDto, blockchain: BlockchainDto): UnionPendingOrderDto {
        return when (source) {
            is OrderSideMatchDto -> UnionPendingOrderMatchDto(
                id = UnionOrderIdDto(blockchain, EthConverter.convert(source.hash)),
                make = source.make?.let { EthConverter.convert(it, blockchain) },
                take = source.take?.let { EthConverter.convert(it, blockchain) },
                date = source.date,
                side = source.side?.let { convert(it) },
                fill = source.fill,
                maker = source.maker?.let { UnionAddressConverter.convert(it, blockchain) },
                taker = source.taker?.let { UnionAddressConverter.convert(it, blockchain) },
                counterHash = source.counterHash?.let { EthConverter.convert(it) },
                makeUsd = source.makeUsd,
                takeUsd = source.takeUsd,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd
            )
            is OrderCancelDto -> UnionPendingOrderCancelDto(
                id = UnionOrderIdDto(blockchain, EthConverter.convert(source.hash)),
                make = source.make?.let { EthConverter.convert(it, blockchain) },
                take = source.take?.let { EthConverter.convert(it, blockchain) },
                date = source.date,
                maker = source.maker?.let { UnionAddressConverter.convert(it, blockchain) },
                owner = source.owner?.let { UnionAddressConverter.convert(it, blockchain) }
            )
            is OnChainOrderDto -> UnionOnChainOrderDto(
                id = UnionOrderIdDto(blockchain, EthConverter.convert(source.hash)),
                make = source.make?.let { EthConverter.convert(it, blockchain) },
                take = source.take?.let { EthConverter.convert(it, blockchain) },
                date = source.date,
                maker = source.maker?.let { UnionAddressConverter.convert(it, blockchain) }
            )
        }
    }

}

