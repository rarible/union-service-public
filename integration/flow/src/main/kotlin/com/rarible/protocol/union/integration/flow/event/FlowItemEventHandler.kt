package com.rarible.protocol.union.integration.flow.event

import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.converter.FlowItemConverter
import org.slf4j.LoggerFactory

open class FlowItemEventHandler(
    override val handler: IncomingEventHandler<UnionItemEvent>
) : AbstractBlockchainEventHandler<FlowNftItemEventDto, UnionItemEvent>(
    BlockchainDto.FLOW,
    EventType.ITEM
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: FlowNftItemEventDto): UnionItemEvent {
        logger.info("Received {} Item event: {}", blockchain, event)
        return FlowItemConverter.convert(event, blockchain)
    }
}
