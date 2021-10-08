package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import org.springframework.stereotype.Component

@Component
class EthOrderEventConverter(
    private val ethOrderConverter: EthOrderConverter
) {

    suspend fun convert(source: com.rarible.protocol.dto.OrderEventDto, blockchain: BlockchainDto): OrderEventDto {
        return when (source) {
            is com.rarible.protocol.dto.OrderUpdateEventDto -> {
                val order = ethOrderConverter.convert(source.order, blockchain)
                OrderUpdateEventDto(
                    eventId = source.eventId,
                    orderId = order.id,
                    order = order
                )
            }
        }
    }
}

