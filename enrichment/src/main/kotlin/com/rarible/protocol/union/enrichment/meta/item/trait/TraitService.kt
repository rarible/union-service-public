package com.rarible.protocol.union.enrichment.meta.item.trait

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.model.ItemAttributeCountChange
import com.rarible.protocol.union.enrichment.repository.TraitRepository
import org.springframework.stereotype.Component

@Component
class TraitService(
    private val traitRepository: TraitRepository,
) {
    suspend fun changeItemsCount(
        collectionId: CollectionIdDto,
        changes: Set<ItemAttributeCountChange>,
    ) {
        changes.forEach {
            val traitId = traitRepository.incrementItemsCount(
                collectionId,
                it.attribute,
                incTotal = it.totalChange,
                incListed = it.listedChange
            )
            indexTrait(traitId)
        }
    }

    private suspend fun indexTrait(traitId: String) {
        // TODO: Index trait
    }
}
