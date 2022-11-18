package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
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
) : AbstractBlockchainEventHandler<NftOwnershipEventDto, UnionOwnershipEvent>(blockchain) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun handleInternal(event: NftOwnershipEventDto) = handler.onEvent(convert(event))
    suspend fun handleInternal(events: List<NftOwnershipEventDto>) = handler.onEvents(events.map(this::convert))

    private fun convert(event: NftOwnershipEventDto): UnionOwnershipEvent {
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
) : EthOwnershipEventHandler(BlockchainDto.ETHEREUM, handler) {

    @CaptureTransaction("OwnershipEvent#ETHEREUM")
    override suspend fun handle(event: NftOwnershipEventDto) = handleInternal(event)

    @CaptureTransaction("OwnershipsEvents#ETHEREUM")
    override suspend fun handle(events: List<NftOwnershipEventDto>) = handleInternal(events)
}

open class PolygonOwnershipEventHandler(
    handler: IncomingEventHandler<UnionOwnershipEvent>
) : EthOwnershipEventHandler(BlockchainDto.POLYGON, handler) {

    @CaptureTransaction("OwnershipEvent#POLYGON")
    override suspend fun handle(event: NftOwnershipEventDto) = handleInternal(event)

    @CaptureTransaction("OwnershipEvents#POLYGON")
    override suspend fun handle(events: List<NftOwnershipEventDto>) = handleInternal(events)
}
