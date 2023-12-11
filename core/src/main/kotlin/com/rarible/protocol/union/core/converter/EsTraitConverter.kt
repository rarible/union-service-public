package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.model.UnionTraitEvent
import com.rarible.protocol.union.core.model.elastic.EsTrait

object EsTraitConverter {

    fun UnionTraitEvent.toEsTrait(): EsTrait {
        return EsTrait(
            id = id,
            blockchain = collectionId.blockchain,
            collection = collectionId.fullId(),
            key = key,
            value = value,
            itemsCount = itemsCount,
            listedItemsCount = listedItemsCount,
            version = version
        )
    }
}
