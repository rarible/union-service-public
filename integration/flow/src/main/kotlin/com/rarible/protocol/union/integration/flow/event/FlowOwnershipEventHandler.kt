package com.rarible.protocol.union.integration.flow.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.dto.FlowNftOwnershipDeleteEventDto
import com.rarible.protocol.dto.FlowNftOwnershipUpdateEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.integration.flow.converter.FlowOwnershipConverter
import org.slf4j.LoggerFactory

open class FlowOwnershipEventHandler(
    override val handler: IncomingEventHandler<UnionOwnershipEvent>
) : AbstractBlockchainEventHandler<FlowOwnershipEventDto, UnionOwnershipEvent>(BlockchainDto.FLOW) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("OwnershipEvent#FLOW")
    override suspend fun handleSafely(event: FlowOwnershipEventDto) {
        logger.debug("Received Flow Ownership event: type={}", event::class.java.simpleName)

        when (event) {
            is FlowNftOwnershipUpdateEventDto -> {
                val unionOwnership = FlowOwnershipConverter.convert(event.ownership, blockchain)
                handler.onEvent(UnionOwnershipUpdateEvent(unionOwnership))
            }
            is FlowNftOwnershipDeleteEventDto -> {
                val ownershipId = OwnershipIdDto(
                    blockchain = blockchain,
                    token = UnionAddress(blockchain, event.ownership.contract),
                    tokenId = event.ownership.tokenId,
                    owner = UnionAddress(blockchain, event.ownership.owner)
                )
                handler.onEvent(UnionOwnershipDeleteEvent(ownershipId))
            }
        }
    }

}
