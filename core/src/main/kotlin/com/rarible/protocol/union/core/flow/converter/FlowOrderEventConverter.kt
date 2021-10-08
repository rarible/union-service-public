package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderUpdateEventDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import org.springframework.stereotype.Component

@Component
class FlowOrderEventConverter(
    private val flowOrderConverter: FlowOrderConverter
) {

    suspend fun convert(source: FlowOrderEventDto, blockchain: BlockchainDto): OrderEventDto {
        return when (source) {
            is FlowOrderUpdateEventDto -> {
                val order = flowOrderConverter.convert(source.order, blockchain)
                OrderUpdateEventDto(
                    eventId = source.eventId,
                    orderId = order.id,
                    order = order
                )
            }
        }
    }

}

