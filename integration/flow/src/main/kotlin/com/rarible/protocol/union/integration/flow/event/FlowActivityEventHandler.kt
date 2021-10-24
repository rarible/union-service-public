package com.rarible.protocol.union.integration.flow.event

import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.FlowComponent
import com.rarible.protocol.union.integration.flow.converter.FlowActivityConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@FlowComponent
class FlowActivityEventHandler(
    override val handler: IncomingEventHandler<ActivityDto>,
    private val flowActivityConverter: FlowActivityConverter
) : BlockchainEventHandler<FlowActivityDto, ActivityDto>(BlockchainDto.FLOW) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowActivityDto) {
        logger.debug("Received Flow ({}) Activity event: type={}", event::class.java.simpleName)

        val unionEventDto = flowActivityConverter.convert(event, blockchain)

        handler.onEvent(unionEventDto)
    }
}
