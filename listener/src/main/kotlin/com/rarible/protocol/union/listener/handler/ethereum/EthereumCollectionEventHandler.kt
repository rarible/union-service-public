package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.protocol.dto.NftCollectionEventDto
import com.rarible.protocol.dto.NftCollectionUpdateEventDto
import com.rarible.protocol.union.core.ethereum.converter.EthCollectionConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import com.rarible.protocol.union.listener.service.EnrichmentCollectionEventService
import org.slf4j.LoggerFactory

class EthereumCollectionEventHandler(
    private val collectionEventService: EnrichmentCollectionEventService,
    private val blockchain: BlockchainDto
) : AbstractEventHandler<NftCollectionEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftCollectionEventDto) {
        logger.debug("Received Ethereum ({}) collection event: type={}", blockchain, event::class.java.simpleName)
        when (event) {
            is NftCollectionUpdateEventDto -> {
                val collection = EthCollectionConverter.convert(event.collection, blockchain)
                collectionEventService.onCollectionUpdated(collection)
            }
        }
    }
}
