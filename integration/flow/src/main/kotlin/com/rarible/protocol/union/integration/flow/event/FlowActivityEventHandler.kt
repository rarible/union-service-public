package com.rarible.protocol.union.integration.flow.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.converter.FlowActivityConverter
import org.slf4j.LoggerFactory

class FlowActivityEventHandler(
    override val handler: IncomingEventHandler<ActivityDto>,
    private val flowActivityConverter: FlowActivityConverter
) : AbstractBlockchainEventHandler<FlowActivityDto, ActivityDto>(BlockchainDto.FLOW) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("ActivityEvent#FLOW")
    override suspend fun handleSafely(event: FlowActivityDto) {
        logger.debug("Received Flow ({}) Activity event: type={}", event::class.java.simpleName)

        val unionEventDto = flowActivityConverter.convert(event, blockchain)

        handler.onEvent(unionEventDto)
    }
}
