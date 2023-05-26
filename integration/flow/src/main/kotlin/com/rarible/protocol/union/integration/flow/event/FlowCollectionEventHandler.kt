package com.rarible.protocol.union.integration.flow.event

import com.rarible.protocol.dto.FlowCollectionEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.converter.FlowCollectionConverter
import org.slf4j.LoggerFactory

open class FlowCollectionEventHandler(
    override val handler: IncomingEventHandler<UnionCollectionEvent>
) : AbstractBlockchainEventHandler<FlowCollectionEventDto, UnionCollectionEvent>(
    BlockchainDto.FLOW,
    EventType.COLLECTION
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: FlowCollectionEventDto): UnionCollectionEvent? {
        logger.info("Received {} Collection event: {}", blockchain, event)
        return FlowCollectionConverter.convert(event, blockchain)
    }
}
