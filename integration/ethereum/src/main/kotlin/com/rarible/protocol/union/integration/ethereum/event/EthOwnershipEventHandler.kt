package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import org.slf4j.LoggerFactory

class EthOwnershipEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionOwnershipEvent>
) : AbstractBlockchainEventHandler<NftOwnershipEventDto, UnionOwnershipEvent>(
    blockchain,
    EventType.OWNERSHIP
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: NftOwnershipEventDto): UnionOwnershipEvent {
        logger.info("Received {} Ownership event: {}:{}", blockchain, event::class.simpleName, event.ownershipId)
        return EthOwnershipConverter.convert(event, blockchain)
    }
}
