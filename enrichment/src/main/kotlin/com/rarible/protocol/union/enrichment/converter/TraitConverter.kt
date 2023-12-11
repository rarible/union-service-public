package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionTraitEvent
import com.rarible.protocol.union.enrichment.model.Trait

object TraitConverter {

    fun toEvent(trait: Trait): UnionTraitEvent {
        return UnionTraitEvent(
            id = trait.id,
            collectionId = trait.collectionId.toDto(),
            key = trait.key,
            value = trait.value,
            itemsCount = trait.itemsCount,
            listedItemsCount = trait.listedItemsCount,
            version = trait.version
        )
    }

}
