package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexCollection
import com.rarible.protocol.union.integration.immutablex.converter.ImxCollectionConverter

class ImxCollectionEventHandler(
    private val collectionHandler: IncomingEventHandler<UnionCollectionEvent>
) {

    private val blockchain = BlockchainDto.IMMUTABLEX

    suspend fun handle(collections: List<ImmutablexCollection>) {
        collections.forEach { collection ->
            val unionCollection = ImxCollectionConverter.convert(collection, blockchain)
            collectionHandler.onEvent(UnionCollectionUpdateEvent(unionCollection))
        }
    }
}
