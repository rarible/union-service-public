package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.model.elastic.EsTrait
import com.rarible.protocol.union.dto.SearchableTraitEventDto

object EsTraitConverter {

    fun SearchableTraitEventDto.toEsTrait(): EsTrait {
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
