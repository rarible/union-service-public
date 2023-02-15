package com.rarible.protocol.union.integration.flow.event

import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.converter.FlowOrderConverter
import org.slf4j.LoggerFactory

open class FlowOrderEventHandler(
    override val handler: IncomingEventHandler<UnionOrderEvent>,
    private val flowOrderConverter: FlowOrderConverter
) : AbstractBlockchainEventHandler<FlowOrderEventDto, UnionOrderEvent>(
    BlockchainDto.FLOW,
    EventType.ORDER
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: FlowOrderEventDto): UnionOrderEvent {
        logger.info("Received {} Order event: {}", blockchain, event)
        return flowOrderConverter.convert(event, blockchain)
    }
}
