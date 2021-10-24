package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.integration.ethereum.EthereumComponent
import com.rarible.protocol.union.integration.ethereum.PolygonComponent
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

sealed class EthOwnershipEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionOwnershipEvent>
) : BlockchainEventHandler<NftOwnershipEventDto, UnionOwnershipEvent>(blockchain) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftOwnershipEventDto) {
        logger.debug("Received Ethereum ({}) Ownership event: type={}", blockchain, event::class.java.simpleName)

        when (event) {
            is NftOwnershipUpdateEventDto -> {
                val ownership = EthOwnershipConverter.convert(event.ownership, blockchain)
                handler.onEvent(UnionOwnershipUpdateEvent(ownership))
            }
            is NftOwnershipDeleteEventDto -> {
                val ownershipId = OwnershipIdDto(
                    blockchain = blockchain,
                    token = UnionAddress(blockchain, EthConverter.convert(event.ownership.token)),
                    tokenId = event.ownership.tokenId,
                    owner = UnionAddress(blockchain, EthConverter.convert(event.ownership.owner))
                )
                handler.onEvent(UnionOwnershipDeleteEvent(ownershipId))
            }
        }
    }
}

@Component
@EthereumComponent
class EthereumOwnershipEventHandler(
    handler: IncomingEventHandler<UnionOwnershipEvent>
) : EthOwnershipEventHandler(
    BlockchainDto.ETHEREUM,
    handler
)

@Component
@PolygonComponent
class PolygonOwnershipEventHandler(
    handler: IncomingEventHandler<UnionOwnershipEvent>
) : EthOwnershipEventHandler(
    BlockchainDto.POLYGON,
    handler
)
