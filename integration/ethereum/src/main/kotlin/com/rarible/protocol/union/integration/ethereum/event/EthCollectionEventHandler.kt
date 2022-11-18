package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import org.slf4j.LoggerFactory

abstract class EthCollectionEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionCollectionEvent>
) : AbstractBlockchainEventHandler<NftCollectionEventDto, UnionCollectionEvent>(blockchain) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun handleInternal(event: NftCollectionEventDto) = handler.onEvent(convert(event))
    suspend fun handleInternal(events: List<NftCollectionEventDto>) = handler.onEvents(events.map(::convert))

    private fun convert(event: NftCollectionEventDto): UnionCollectionEvent {
        logger.info("Received {} collection event: {}", blockchain, event)

        return when (event) {
            is NftCollectionUpdateEventDto -> {
                val collection = EthCollectionConverter.convert(event.collection, blockchain)
                UnionCollectionUpdateEvent(collection)
            }
        }
    }
}

open class EthereumCollectionEventHandler(
    handler: IncomingEventHandler<UnionCollectionEvent>
) : EthCollectionEventHandler(BlockchainDto.ETHEREUM, handler) {

    @CaptureTransaction("CollectionEvent#ETHEREUM")
    override suspend fun handle(event: NftCollectionEventDto) = handleInternal(event)

    @CaptureTransaction("CollectionEvents#ETHEREUM")
    override suspend fun handle(events: List<NftCollectionEventDto>) = handleInternal(events)
}

open class PolygonCollectionEventHandler(
    handler: IncomingEventHandler<UnionCollectionEvent>
) : EthCollectionEventHandler(BlockchainDto.POLYGON, handler) {

    @CaptureTransaction("CollectionEvent#POLYGON")
    override suspend fun handle(event: NftCollectionEventDto) = handleInternal(event)

    @CaptureTransaction("CollectionEvents#POLYGON")
    override suspend fun handle(events: List<NftCollectionEventDto>) = handleInternal(events)
}