package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.dto.CollectionDto

object EsCollectionConverter {

    fun convert(collection: CollectionDto): EsCollection {
        return EsCollection(
            collectionId = collection.id.fullId(),
            type = collection.type.name,
            blockchain = collection.blockchain.name,
            name = collection.name,
            symbol = collection.symbol,
            owner = collection.owner?.fullId(),
            parent = collection.parent?.fullId(),
            meta = collection.meta?.let {
                EsCollection.CollectionMeta(
                    name = it.name,
                    description = it.description,
                    feeRecipient = it.feeRecipient?.fullId()
                )
            }
        )
    }
}
