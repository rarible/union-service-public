package com.rarible.protocol.union.integration.flow.event

import com.rarible.protocol.dto.FlowActivityEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.converter.FlowActivityConverter
import org.slf4j.LoggerFactory

open class FlowActivityEventHandler(
    override val handler: IncomingEventHandler<UnionActivity>,
    private val flowActivityConverter: FlowActivityConverter
) : AbstractBlockchainEventHandler<FlowActivityEventDto, UnionActivity>(
    BlockchainDto.FLOW,
    EventType.ACTIVITY
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: FlowActivityEventDto): UnionActivity {
        logger.info(
            "Received {} Activity event: {}:{}",
            blockchain,
            event.activity::class.simpleName,
            event.activity.id
        )
        return flowActivityConverter.convert(event)
    }
}
