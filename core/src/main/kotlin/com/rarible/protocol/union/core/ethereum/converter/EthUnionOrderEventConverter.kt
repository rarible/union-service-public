package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.EthOrderUpdateEventDto
import com.rarible.protocol.union.dto.UnionOrderEventDto

object EthUnionOrderEventConverter {

    fun convert(source: OrderEventDto, blockchain: EthBlockchainDto): UnionOrderEventDto {
        return when (source) {
            is OrderUpdateEventDto -> {
                EthOrderUpdateEventDto(
                    eventId = source.eventId,
                    orderId = source.orderId,
                    order = EthUnionOrderConverter.convert(source.order, blockchain)
                )
            }
        }
    }
}

