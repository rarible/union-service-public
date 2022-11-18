package com.rarible.protocol.union.integration.flow.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.dto.FlowNftItemDeleteEventDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemUpdateEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.integration.flow.converter.FlowItemConverter
import org.slf4j.LoggerFactory

open class FlowItemEventHandler(
    override val handler: IncomingEventHandler<UnionItemEvent>
) : AbstractBlockchainEventHandler<FlowNftItemEventDto, UnionItemEvent>(
    BlockchainDto.FLOW
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("ItemEvent#FLOW")
    override suspend fun handle(event: FlowNftItemEventDto) = handler.onEvent(convert(event))

    @CaptureTransaction("ItemEvents#FLOW")
    override suspend fun handle(events: List<FlowNftItemEventDto>) = handler.onEvents(events.map { convert(it) })

    private fun convert(event: FlowNftItemEventDto): UnionItemEvent {
        logger.info("Received {} Item event: {}", blockchain, event)

        return when (event) {
            is FlowNftItemUpdateEventDto -> {
                val item = FlowItemConverter.convert(event.item, blockchain)
                UnionItemUpdateEvent(item)
            }
            is FlowNftItemDeleteEventDto -> {
                val itemId = ItemIdDto(
                    blockchain = blockchain,
                    contract = event.item.token,
                    tokenId = event.item.tokenId.toBigInteger()
                )
                UnionItemDeleteEvent(itemId)
            }
        }
    }
}
