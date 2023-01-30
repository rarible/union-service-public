package com.rarible.protocol.union.integration.solana.event

import com.rarible.protocol.solana.dto.CollectionEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.core.model.stubEventMark
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.solana.converter.SolanaCollectionConverter
import org.slf4j.LoggerFactory

open class SolanaCollectionEventHandler(
    override val handler: IncomingEventHandler<UnionCollectionEvent>
) : AbstractBlockchainEventHandler<CollectionEventDto, UnionCollectionEvent>(
    BlockchainDto.SOLANA,
    EventType.COLLECTION
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: CollectionEventDto): UnionCollectionEvent {
        logger.info("Received {} Collection event: {}", blockchain, event)

        return when (event) {
            is com.rarible.protocol.solana.dto.CollectionUpdateEventDto -> {
                val collection = SolanaCollectionConverter.convert(event.collection, blockchain)
                UnionCollectionUpdateEvent(collection, stubEventMark())
            }
        }
    }
}
