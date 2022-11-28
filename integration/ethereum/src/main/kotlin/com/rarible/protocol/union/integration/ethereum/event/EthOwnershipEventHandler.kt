package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import org.slf4j.LoggerFactory

abstract class EthOwnershipEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionOwnershipEvent>
) : AbstractBlockchainEventHandler<NftOwnershipEventDto, UnionOwnershipEvent>(
    blockchain,
    EventType.OWNERSHIP
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: NftOwnershipEventDto): UnionOwnershipEvent {
        logger.info("Received {} Ownership event: {}", blockchain, event)

        return when (event) {
            is NftOwnershipUpdateEventDto -> {
                val ownership = EthOwnershipConverter.convert(event.ownership, blockchain)
                UnionOwnershipUpdateEvent(ownership)
            }
            is NftOwnershipDeleteEventDto -> {
                val deletedOwnership = event.ownership!!
                val ownershipId = OwnershipIdDto(
                    blockchain = blockchain,
                    contract = EthConverter.convert(deletedOwnership.token),
                    tokenId = deletedOwnership.tokenId,
                    owner = UnionAddressConverter.convert(blockchain, EthConverter.convert(deletedOwnership.owner))
                )
                UnionOwnershipDeleteEvent(ownershipId)
            }
        }
    }
}

open class EthereumOwnershipEventHandler(
    handler: IncomingEventHandler<UnionOwnershipEvent>
) : EthOwnershipEventHandler(BlockchainDto.ETHEREUM, handler)

open class PolygonOwnershipEventHandler(
    handler: IncomingEventHandler<UnionOwnershipEvent>
) : EthOwnershipEventHandler(BlockchainDto.POLYGON, handler)