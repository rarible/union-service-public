package com.rarible.protocol.union.core.converter.flow

import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderUpdateEventDto
import com.rarible.protocol.dto.PayInfoDto
import com.rarible.protocol.union.dto.*
import org.springframework.core.convert.converter.Converter
import java.math.BigDecimal

object FlowUnionOrderEventDtoConverter : Converter<FlowOrderEventDto, UnionOrderEventDto> {

    override fun convert(source: FlowOrderEventDto): UnionOrderEventDto {
        return when (source) {
            is FlowOrderUpdateEventDto -> FlowOrderUpdateEventDto(
                eventId = source.eventId,
                orderId = source.orderId,
                order = FlowOrderDto(
                    id = source.orderId, //TODO: In Eth there is not such filed,
                    type = FlowOrderTypeDto.RARIBLE_FLOW_V1,
                    maker = FlowAddress(source.order.maker),
                    taker = source.order.taker?.let { FlowAddress(it) },
                    make = FlowAssetDtoConverter.convert(source.order.make),
                    take = FlowAssetDtoConverter.convert(source.order.take!!), //TODO: Why take is null?
                    fill = source.order.fill,
                    startedAt = null, //TODO: No needed filed
                    endedAt = null, //TODO: No needed filed
                    makeStock = BigDecimal.ZERO, // TODO: No needed filed
                    cancelled = source.order.cancelled,
                    createdAt = source.order.createdAt,
                    lastUpdatedAt = source.order.lastUpdateAt,
                    makeBalance = BigDecimal.ZERO, //TODO: Need remove
                    makePriceUSD = source.order.amountUsd, //TODO: I think need to rename
                    takePriceUSD = source.order.amountUsd, //TODO: I think need to rename
                    priceHistory = emptyList(),
                    data = convert(source.order.data)
                )
            )
        }
    }

    fun convert(source: com.rarible.protocol.dto.FlowOrderDataDto): FlowOrderDataDto {
        return FlowOrderDataV1Dto(
            payouts = source.payouts.map { convert(it) },
            originFees = source.originalFees.map { convert(it) }
        )
    }


    fun convert(source: PayInfoDto): FlowOrderPayoutDto {
        return FlowOrderPayoutDto(
            account = FlowAddressConverter.convert(source.account),
            value = source.value.toBigInteger() // TODO
        )
    }
}

