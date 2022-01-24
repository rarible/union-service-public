package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import org.slf4j.LoggerFactory

abstract class EthCollectionEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<CollectionEventDto>
) : AbstractBlockchainEventHandler<NftCollectionEventDto, CollectionEventDto>(blockchain) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun handleInternal(event: NftCollectionEventDto) {
        logger.debug("Received Ethereum ({}) collection event: type={}", blockchain, event)

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

open class EthereumCollectionEventHandler(
    handler: IncomingEventHandler<CollectionEventDto>
) : EthCollectionEventHandler(BlockchainDto.ETHEREUM, handler) {
    @CaptureTransaction("CollectionEvent#ETHEREUM")
    override suspend fun handle(event: NftCollectionEventDto) = handleInternal(event)
}

open class PolygonCollectionEventHandler(
    handler: IncomingEventHandler<CollectionEventDto>
) : EthCollectionEventHandler(BlockchainDto.POLYGON, handler) {
    @CaptureTransaction("CollectionEvent#POLYGON")
    override suspend fun handle(event: NftCollectionEventDto) = handleInternal(event)
}