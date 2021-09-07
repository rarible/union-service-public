package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderUpdateEventDto
import com.rarible.protocol.union.dto.*

object FlowUnionOrderEventConverter {

    fun convert(source: FlowOrderEventDto, blockchain: FlowBlockchainDto): UnionOrderEventDto {
        return when (source) {
            is FlowOrderUpdateEventDto -> FlowOrderUpdateEventDto(
                eventId = source.eventId,
                orderId = source.orderId,
                order = FlowUnionOrderConverter.convert(source.order, blockchain)
            )
        }
    }

}

