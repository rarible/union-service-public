package com.rarible.protocol.union.listener.handler.flow

import com.rarible.protocol.dto.FlowNftOwnershipDeleteEventDto
import com.rarible.protocol.dto.FlowNftOwnershipUpdateEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.union.core.flow.converter.FlowOwnershipConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import com.rarible.protocol.union.listener.service.EnrichmentOwnershipEventService
import org.slf4j.LoggerFactory

class FlowOwnershipEventHandler(
    private val ownershipEventService: EnrichmentOwnershipEventService,
    private val blockchain: BlockchainDto
) : AbstractEventHandler<FlowOwnershipEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowOwnershipEventDto) {
        logger.debug("Received Flow Ownership event: type={}", event::class.java.simpleName)

        when (event) {
            is FlowNftOwnershipUpdateEventDto -> {
                val item = FlowOwnershipConverter.convert(event.ownership, blockchain)
                ownershipEventService.onOwnershipUpdated(item)
            }
            is FlowNftOwnershipDeleteEventDto -> {
                val ownershipId = OwnershipIdDto(
                    blockchain = blockchain,
                    token = UnionAddress(blockchain, event.ownership.contract),
                    tokenId = event.ownership.tokenId,
                    owner = UnionAddress(blockchain, event.ownership.owner)
                )
                ownershipEventService.onOwnershipDeleted(ownershipId)
            }
        }
    }

}
