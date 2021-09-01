package com.rarible.protocol.union.core.converter.flow

import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderUpdateEventDto
import com.rarible.protocol.union.dto.*
import org.springframework.core.convert.converter.Converter
import java.math.BigDecimal
import java.math.BigInteger

object FlowUnionOrderEventDtoConverter : Converter<FlowOrderEventDto, UnionOrderEventDto> {
    override fun convert(source: FlowOrderEventDto): UnionOrderEventDto {
        return when (source) {
            is FlowOrderUpdateEventDto -> FlowOrderUpdateEventDto(
                eventId = source.eventId,
                orderId = source.orderId,
                order = FlowOrderDto(
                    id = BigInteger.ONE, //TODO: In Eth there is not such filed,
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
                    data = FlowOrderDataV1Dto(
                        //TODO: Maybe data should be Sealed class like Eth
                        dataType = FlowOrderDataV1Dto.DataType.DATA_V1,
                        payouts = source.order.data.payouts.map { TODO() },
                        originFees = source.order.data.originalFees.map { TODO() }
                    )
                )
            )
        }
    }
}

