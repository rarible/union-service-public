package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.dto.CollectionDto
import java.time.Instant

object EsCollectionConverter {

    fun convert(collection: CollectionDto): EsCollection {
        return EsCollection(
            collectionId = collection.id.fullId(),
            date = Instant.now(), // TODO fill from CollectionDto when lastUpdatedAt/dbUpdateAt is added
            blockchain = collection.blockchain,
            name = collection.name,
            symbol = collection.symbol,
            owner = collection.owner?.fullId(),
            meta = collection.meta?.let {
                EsCollection.CollectionMeta(
                    name = it.name,
                    description = it.description,
                )
            }
        )
    }
}
