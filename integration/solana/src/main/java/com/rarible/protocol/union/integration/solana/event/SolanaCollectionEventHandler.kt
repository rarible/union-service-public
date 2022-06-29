package com.rarible.protocol.union.integration.solana.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.solana.converter.SolanaCollectionConverter
import org.slf4j.LoggerFactory

open class SolanaCollectionEventHandler(
    override val handler: IncomingEventHandler<UnionCollectionEvent>
) : AbstractBlockchainEventHandler<com.rarible.protocol.solana.dto.CollectionEventDto, UnionCollectionEvent>(
    BlockchainDto.SOLANA
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("CollectionEvent#SOLANA")
    override suspend fun handle(event: com.rarible.protocol.solana.dto.CollectionEventDto) {
        logger.info("Received {} Collection event: {}", blockchain, event)

        when (event) {
            is com.rarible.protocol.solana.dto.CollectionUpdateEventDto -> {
                val collection = SolanaCollectionConverter.convert(event.collection, blockchain)
                val unionCollectionEvent = UnionCollectionUpdateEvent(collection)
                handler.onEvent(unionCollectionEvent)
            }
        }
    }
}
