package com.rarible.protocol.union.integration.flow.event

import com.rarible.protocol.dto.FlowNftOwnershipDeleteEventDto
import com.rarible.protocol.dto.FlowNftOwnershipUpdateEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.core.model.stubEventMark
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.integration.flow.converter.FlowOwnershipConverter
import org.slf4j.LoggerFactory

open class FlowOwnershipEventHandler(
    override val handler: IncomingEventHandler<UnionOwnershipEvent>
) : AbstractBlockchainEventHandler<FlowOwnershipEventDto, UnionOwnershipEvent>(
    BlockchainDto.FLOW,
    EventType.OWNERSHIP
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: FlowOwnershipEventDto): UnionOwnershipEvent? {
        logger.info("Received {} Ownership event: {}", blockchain, event)

        return when (event) {
            is FlowNftOwnershipUpdateEventDto -> {
                val unionOwnership = FlowOwnershipConverter.convert(event.ownership, blockchain)
                UnionOwnershipUpdateEvent(unionOwnership, stubEventMark())
            }
            is FlowNftOwnershipDeleteEventDto -> {
                val ownershipId = OwnershipIdDto(
                    blockchain = blockchain,
                    contract = event.ownership.contract,
                    tokenId = event.ownership.tokenId,
                    owner = UnionAddressConverter.convert(blockchain, event.ownership.owner)
                )
                UnionOwnershipDeleteEvent(ownershipId, stubEventMark())
            }
        }
    }

}
