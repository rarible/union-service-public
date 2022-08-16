package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.immutablex.cache.ImxCollectionCreator
import com.rarible.protocol.union.integration.immutablex.cache.ImxCollectionCreatorRepository
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexCollection
import com.rarible.protocol.union.integration.immutablex.converter.ImxCollectionConverter

class ImxCollectionEventHandler(
    private val collectionHandler: IncomingEventHandler<UnionCollectionEvent>,
    private val collectionCreatorRepository: ImxCollectionCreatorRepository
) {

    private val blockchain = BlockchainDto.IMMUTABLEX

    suspend fun handle(collections: List<ImmutablexCollection>) {
        val creators = collections.mapNotNull { collection ->
            val unionCollection = ImxCollectionConverter.convert(collection, blockchain)
            collectionHandler.onEvent(UnionCollectionUpdateEvent(unionCollection))
            collection.projectOwnerAddress?.let { ImxCollectionCreator(collection.address, it) }
        }
        collectionCreatorRepository.saveAll(creators)
    }
}
