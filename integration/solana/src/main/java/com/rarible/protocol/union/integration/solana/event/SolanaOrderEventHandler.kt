package com.rarible.protocol.union.integration.solana.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.solana.dto.OrderEventDto
import com.rarible.protocol.solana.dto.OrderUpdateEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.solana.converter.SolanaOrderConverter
import org.slf4j.LoggerFactory

open class SolanaOrderEventHandler(
    override val handler: IncomingEventHandler<UnionOrderEvent>,
    private val solanaOrderConverter: SolanaOrderConverter
) : AbstractBlockchainEventHandler<OrderEventDto, UnionOrderEvent>(BlockchainDto.SOLANA) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("OrderEvent#SOLANA")
    override suspend fun handle(event: OrderEventDto) = handler.onEvent(convert(event))

    @CaptureTransaction("OrderEvents#SOLANA")
    override suspend fun handle(events: List<OrderEventDto>) = handler.onEvents(events.map { convert(it) })

    private suspend fun convert(event: OrderEventDto): UnionOrderEvent {
        logger.info("Received {} Order event: {}", blockchain, event)

        return when (event) {
            is OrderUpdateEventDto -> {
                val unionOrder = solanaOrderConverter.convert(event.order, blockchain)
                UnionOrderUpdateEvent(unionOrder)
            }
        }
    }
}
