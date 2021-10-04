package com.rarible.protocol.union.listener.handler.flow

import com.rarible.protocol.dto.FlowNftItemDeleteEventDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemUpdateEventDto
import com.rarible.protocol.union.core.flow.converter.FlowItemConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import com.rarible.protocol.union.listener.service.EnrichmentItemEventService
import org.slf4j.LoggerFactory

class FlowItemEventHandler(
    private val itemEventService: EnrichmentItemEventService,
    private val blockchain: BlockchainDto
) : AbstractEventHandler<FlowNftItemEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowNftItemEventDto) {
        logger.debug("Received Flow item event: type={}", event::class.java.simpleName)

        when (event) {
            is FlowNftItemUpdateEventDto -> {
                val item = FlowItemConverter.convert(event.item, blockchain)
                itemEventService.onItemUpdated(item)
            }
            is FlowNftItemDeleteEventDto -> {
                val itemId = ItemIdDto(
                    blockchain = blockchain,
                    token = UnionAddress(blockchain, event.item.token),
                    tokenId = event.item.tokenId.toBigInteger()
                )
                itemEventService.onItemDeleted(itemId)
            }
        }
    }
}
