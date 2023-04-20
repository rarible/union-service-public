package com.rarible.protocol.union.core.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.elastic.EsCollection
import com.rarible.protocol.union.dto.CollectionDto

object EsCollectionConverter {

    fun convert(collection: CollectionDto): EsCollection {
        return EsCollection(
            collectionId = collection.id.fullId(),
            date = nowMillis(), // TODO fill from CollectionDto when lastUpdatedAt/dbUpdateAt is added
            blockchain = collection.blockchain,
            name = collection.name,
            symbol = collection.symbol,
            owner = collection.owner?.fullId(),
            meta = collection.meta?.let {
                EsCollection.CollectionMeta(
                    name = it.name,
                )
            },
            self = collection.self
        )
    }
}
