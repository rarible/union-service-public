package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.PayInfoDto
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.flow.FlowOrderIdDto
import java.math.BigInteger
import java.time.Instant

object FlowUnionOrderConverter {

    fun convert(order: com.rarible.protocol.dto.FlowOrderDto, blockchain: FlowBlockchainDto): FlowOrderDto {
        return FlowOrderV1Dto(
            id = FlowOrderIdDto(blockchain, order.id.toString()),
            maker = FlowAddressConverter.convert(order.maker, blockchain),
            taker = order.taker?.let { FlowAddressConverter.convert(it, blockchain) },
            make = FlowConverter.convert(order.make, blockchain),
            take = FlowConverter.convert(order.take, blockchain),
            fill = order.fill.toBigInteger(),
            startedAt = order.start,
            endedAt = order.end,
            makeStock = order.makeStock,
            cancelled = order.cancelled,
            createdAt = order.createdAt,
            lastUpdatedAt = order.lastUpdateAt,
            makePriceUsd = order.priceUsd,
            takePriceUsd = order.priceUsd,
            priceHistory = emptyList(),
            data = convert(order.data, blockchain)
        )
    }

    private fun convert(
        source: com.rarible.protocol.dto.FlowOrderDataDto,
        blockchain: FlowBlockchainDto
    ): FlowOrderDataV1Dto {
        return FlowOrderDataV1Dto(
            payouts = source.payouts.map { convert(it, blockchain) },
            originFees = source.originalFees.map { convert(it, blockchain) }
        )
    }

    private fun convert(source: PayInfoDto, blockchain: FlowBlockchainDto): FlowOrderPayoutDto {
        return FlowOrderPayoutDto(
            account = FlowAddressConverter.convert(source.account, blockchain),
            value = source.value.toBigInteger()
        )
    }

}

