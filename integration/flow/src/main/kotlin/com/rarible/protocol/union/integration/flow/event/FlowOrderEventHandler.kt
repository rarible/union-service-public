package com.rarible.protocol.union.integration.flow.event

import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderUpdateEventDto
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.converter.FlowOrderConverter
import org.slf4j.LoggerFactory

class FlowOrderEventHandler(
    override val handler: IncomingEventHandler<UnionOrderEvent>,
    private val flowOrderConverter: FlowOrderConverter
) : BlockchainEventHandler<FlowOrderEventDto, UnionOrderEvent>(BlockchainDto.FLOW) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowOrderEventDto) {
        logger.debug("Received Flow Order event: type={}", event::class.java.simpleName)

        when (event) {
            is FlowOrderUpdateEventDto -> {
                val unionOrder = flowOrderConverter.convert(event.order, blockchain)
                handler.onEvent(UnionOrderUpdateEvent(unionOrder))
            }
        }
    }
}
