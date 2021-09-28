package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowOrderDto
import com.rarible.protocol.dto.PayInfoDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowOrderDataV1Dto
import com.rarible.protocol.union.dto.UnionOrderDto
import com.rarible.protocol.union.dto.FlowOrderPayoutDto
import com.rarible.protocol.union.dto.UnionOrderIdDto
import java.math.BigInteger
import java.time.Instant

object FlowUnionOrderConverter {

    fun convert(order: FlowOrderDto, blockchain: BlockchainDto): UnionOrderDto {
        return UnionOrderDto(
            id = UnionOrderIdDto(blockchain, order.id.toString()),
            maker = UnionAddressConverter.convert(order.maker, blockchain),
            taker = order.taker?.let { UnionAddressConverter.convert(it, blockchain) },
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
            data = convert(order.data, blockchain),
            salt = ""// TODO возможно будет на flow?
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

    private fun convert(source: PayInfoDto, blockchain: BlockchainDto): FlowOrderPayoutDto {
        return FlowOrderPayoutDto(
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = source.value.toBigInteger()
        )
    }
}

