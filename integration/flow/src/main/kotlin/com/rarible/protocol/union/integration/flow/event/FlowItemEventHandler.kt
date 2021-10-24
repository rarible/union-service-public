package com.rarible.protocol.union.integration.flow.event

import com.rarible.protocol.dto.FlowNftItemDeleteEventDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemUpdateEventDto
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.integration.flow.FlowComponent
import com.rarible.protocol.union.integration.flow.converter.FlowItemConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@FlowComponent
class FlowItemEventHandler(
    override val handler: IncomingEventHandler<UnionItemEvent>
) : BlockchainEventHandler<FlowNftItemEventDto, UnionItemEvent>(BlockchainDto.FLOW) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowNftItemEventDto) {
        logger.debug("Received Flow item event: type={}", event::class.java.simpleName)

        when (event) {
            is FlowNftItemUpdateEventDto -> {
                val item = FlowItemConverter.convert(event.item, blockchain)
                handler.onEvent(UnionItemUpdateEvent(item))
            }
            is FlowNftItemDeleteEventDto -> {
                val itemId = ItemIdDto(
                    blockchain = blockchain,
                    token = UnionAddress(blockchain, event.item.token),
                    tokenId = event.item.tokenId.toBigInteger()
                )
                handler.onEvent(UnionItemDeleteEvent(itemId))
            }
        }
    }
}
