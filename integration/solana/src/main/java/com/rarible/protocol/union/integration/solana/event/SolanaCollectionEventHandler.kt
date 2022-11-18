package com.rarible.protocol.union.integration.solana.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.solana.dto.CollectionEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.solana.converter.SolanaCollectionConverter
import org.slf4j.LoggerFactory

open class SolanaCollectionEventHandler(
    override val handler: IncomingEventHandler<UnionCollectionEvent>
) : AbstractBlockchainEventHandler<CollectionEventDto, UnionCollectionEvent>(
    BlockchainDto.SOLANA
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("CollectionEvent#SOLANA")
    override suspend fun handle(event: CollectionEventDto) = handler.onEvent(convert(event))

    @CaptureTransaction("CollectionEvents#SOLANA")
    override suspend fun handle(events: List<CollectionEventDto>) = handler.onEvents(events.map { convert(it) })

    private fun convert(event: CollectionEventDto): UnionCollectionEvent {
        logger.info("Received {} Collection event: {}", blockchain, event)

        return when (event) {
            is com.rarible.protocol.solana.dto.CollectionUpdateEventDto -> {
                val collection = SolanaCollectionConverter.convert(event.collection, blockchain)
                UnionCollectionUpdateEvent(collection)
            }
        }
    }
}
