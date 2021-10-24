package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.integration.ethereum.EthereumComponent
import com.rarible.protocol.union.integration.ethereum.PolygonComponent
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

sealed class EthCollectionEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<CollectionEventDto>
) : BlockchainEventHandler<NftCollectionEventDto, CollectionEventDto>(blockchain) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftCollectionEventDto) {
        logger.debug("Received Ethereum ({}) collection event: type={}", blockchain, event::class.java.simpleName)

        when (event) {
            is NftCollectionUpdateEventDto -> {
                val collection = EthCollectionConverter.convert(event.collection, blockchain)
                val unionCollectionEvent = CollectionUpdateEventDto(
                    collectionId = collection.id,
                    eventId = event.eventId,
                    collection = collection
                )
                handler.onEvent(unionCollectionEvent)
            }
        }
    }
}

@Component
@EthereumComponent
class EthereumCollectionEventHandler(
    handler: IncomingEventHandler<CollectionEventDto>
) : EthCollectionEventHandler(
    BlockchainDto.ETHEREUM,
    handler
)

@Component
@PolygonComponent
class PolygonCollectionEventHandler(
    handler: IncomingEventHandler<CollectionEventDto>
) : EthCollectionEventHandler(
    BlockchainDto.POLYGON,
    handler
)
