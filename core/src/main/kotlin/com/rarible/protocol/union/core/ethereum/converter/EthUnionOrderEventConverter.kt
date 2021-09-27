package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthOrderUpdateEventDto
import com.rarible.protocol.union.dto.UnionOrderEventDto

object EthUnionOrderEventConverter {

    fun convert(source: OrderEventDto, blockchain: BlockchainDto): UnionOrderEventDto {
        return when (source) {
            is OrderUpdateEventDto -> {
                val order = EthUnionOrderConverter.convert(source.order, blockchain)
                EthOrderUpdateEventDto(
                    eventId = source.eventId,
                    orderId = order.id,
                    order = order
                )
            }
        }
    }
}

