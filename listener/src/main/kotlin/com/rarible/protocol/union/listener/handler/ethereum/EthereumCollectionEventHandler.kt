package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.union.core.ethereum.converter.EthCollectionConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.enrichment.event.KafkaEventFactory
import com.rarible.protocol.union.listener.handler.BlockchainEventHandler
import org.slf4j.LoggerFactory

class EthereumCollectionEventHandler(
    override val blockchain: BlockchainDto,
    private val producer: RaribleKafkaProducer<CollectionEventDto>
) : BlockchainEventHandler<NftCollectionEventDto>() {

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
                producer.send(KafkaEventFactory.collectionEvent(unionCollectionEvent))
            }
        }
    }
}
