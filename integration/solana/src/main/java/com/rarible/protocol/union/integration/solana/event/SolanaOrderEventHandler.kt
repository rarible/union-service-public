package com.rarible.protocol.union.integration.solana.event

import com.rarible.protocol.solana.dto.OrderEventDto
import com.rarible.protocol.solana.dto.OrderUpdateEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.core.model.stubEventMark
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.solana.converter.SolanaOrderConverter
import org.slf4j.LoggerFactory

open class SolanaOrderEventHandler(
    override val handler: IncomingEventHandler<UnionOrderEvent>,
    private val solanaOrderConverter: SolanaOrderConverter
) : AbstractBlockchainEventHandler<OrderEventDto, UnionOrderEvent>(
    BlockchainDto.SOLANA,
    EventType.ORDER
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: OrderEventDto): UnionOrderEvent {
        logger.info("Received {} Order event: {}", blockchain, event)

        return when (event) {
            is OrderUpdateEventDto -> {
                val unionOrder = solanaOrderConverter.convert(event.order, blockchain)
                UnionOrderUpdateEvent(unionOrder, stubEventMark())
            }
        }
    }
}
