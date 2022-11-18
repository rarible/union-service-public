package com.rarible.protocol.union.integration.flow.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.converter.FlowActivityConverter
import org.slf4j.LoggerFactory

open class FlowActivityEventHandler(
    override val handler: IncomingEventHandler<ActivityDto>,
    private val flowActivityConverter: FlowActivityConverter
) : AbstractBlockchainEventHandler<FlowActivityDto, ActivityDto>(
    BlockchainDto.FLOW
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("ActivityEvent#FLOW")
    override suspend fun handle(event: FlowActivityDto) = handler.onEvent(convert(event))

    @CaptureTransaction("ActivityEvents#FLOW")
    override suspend fun handle(events: List<FlowActivityDto>) = handler.onEvents(
        events.map { convert(it) }
    )

    suspend fun convert(event: FlowActivityDto): ActivityDto {
        logger.debug("Received Flow ({}) Activity event: type={}", event::class.java.simpleName)
        return flowActivityConverter.convert(event)
    }
}
