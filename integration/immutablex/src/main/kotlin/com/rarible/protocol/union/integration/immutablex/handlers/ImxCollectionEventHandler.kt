package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.core.model.blockchainAndIndexerMarks
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexCollection
import com.rarible.protocol.union.integration.immutablex.converter.ImxCollectionConverter
import com.rarible.protocol.union.integration.immutablex.model.ImxCollectionCreator
import com.rarible.protocol.union.integration.immutablex.repository.ImxCollectionCreatorRepository

class ImxCollectionEventHandler(
    private val collectionHandler: IncomingEventHandler<UnionCollectionEvent>,
    private val collectionCreatorRepository: ImxCollectionCreatorRepository
) {

    private val blockchain = BlockchainDto.IMMUTABLEX

    suspend fun handle(collections: List<ImmutablexCollection>) {
        val creators = collections.mapNotNull { collection ->
            val unionCollection = ImxCollectionConverter.convert(collection, blockchain)
            // Potentially could make metrics not realistic
            val eventTimeMarks = blockchainAndIndexerMarks(
                collection.updatedAt ?: collection.createdAt ?: nowMillis()
            )
            collectionHandler.onEvent(UnionCollectionUpdateEvent(unionCollection, eventTimeMarks))
            collection.projectOwnerAddress?.let { ImxCollectionCreator(collection.address, it) }
        }
        collectionCreatorRepository.saveAll(creators)
    }
}
