package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthCollectionConverter
import org.slf4j.LoggerFactory

abstract class EthCollectionEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionCollectionEvent>
) : AbstractBlockchainEventHandler<NftCollectionEventDto, UnionCollectionEvent>(
    blockchain,
    EventType.COLLECTION
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: NftCollectionEventDto): UnionCollectionEvent {
        logger.info("Received {} collection event: {}", blockchain, event)
        return EthCollectionConverter.convert(event, blockchain)
    }
}

open class EthereumCollectionEventHandler(
    handler: IncomingEventHandler<UnionCollectionEvent>
) : EthCollectionEventHandler(BlockchainDto.ETHEREUM, handler)

open class PolygonCollectionEventHandler(
    handler: IncomingEventHandler<UnionCollectionEvent>
) : EthCollectionEventHandler(BlockchainDto.POLYGON, handler)