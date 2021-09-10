package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.PayInfoDto
import com.rarible.protocol.union.dto.*
import java.math.BigDecimal
import java.math.BigInteger

object FlowUnionOrderConverter {

    fun convert(order: com.rarible.protocol.dto.FlowOrderDto, blockchain: FlowBlockchainDto): FlowOrderDto {
        return FlowOrderDto(
            id = FlowOrderIdDto(order.id.toString(), blockchain),
            type = FlowOrderTypeDto.RARIBLE_FLOW_V1,
            maker = FlowAddressConverter.convert(order.maker, blockchain),
            taker = order.taker?.let { FlowAddressConverter.convert(it, blockchain) },
            make = FlowConverter.convert(order.make, blockchain),
            take = FlowConverter.convert(order.take!!, blockchain), //TODO: Why take is null?
            fill = order.fill,
            startedAt = null, //TODO: No needed filed
            endedAt = null, //TODO: No needed filed
            makeStock = BigInteger.ZERO, // TODO: No needed filed
            cancelled = order.cancelled,
            createdAt = order.createdAt,
            lastUpdatedAt = order.lastUpdateAt,
            makeBalance = BigDecimal.ZERO, //TODO: Need remove
            makePriceUsd = order.amountUsd, //TODO: I think need to rename
            takePriceUsd = order.amountUsd, //TODO: I think need to rename
            priceHistory = emptyList(),
            data = convert(order.data, blockchain)
        )
    }

    private fun convert(
        source: com.rarible.protocol.dto.FlowOrderDataDto,
        blockchain: FlowBlockchainDto
    ): FlowOrderDataDto {
        return FlowOrderDataV1Dto(
            payouts = source.payouts.map { convert(it, blockchain) },
            originFees = source.originalFees.map { convert(it, blockchain) }
        )
    }

    private fun convert(source: PayInfoDto, blockchain: FlowBlockchainDto): FlowOrderPayoutDto {
        return FlowOrderPayoutDto(
            account = FlowAddressConverter.convert(source.account, blockchain),
            value = source.value.toBigInteger() // TODO
        )
    }

}

