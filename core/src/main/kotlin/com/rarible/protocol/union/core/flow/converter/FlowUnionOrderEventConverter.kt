package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderUpdateEventDto
import com.rarible.protocol.union.dto.*

object FlowUnionOrderEventConverter {

    fun convert(source: FlowOrderEventDto, blockchain: BlockchainDto): UnionOrderEventDto {
        return when (source) {
            is FlowOrderUpdateEventDto -> {
                val order = FlowUnionOrderConverter.convert(source.order, blockchain)
                UnionOrderUpdateEventDto(
                    eventId = source.eventId,
                    orderId = order.id,
                    order = order
                )
            }
        }
    }

}

