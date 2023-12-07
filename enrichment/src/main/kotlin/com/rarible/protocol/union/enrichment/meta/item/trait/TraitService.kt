package com.rarible.protocol.union.enrichment.meta.item.trait

import com.rarible.core.common.asyncBatchHandle
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
        changes.asyncBatchHandle(TRAIT_HANDLE_BATCH) {
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

    private companion object {
        // We don't expect that there will be more than 500 traits in one collection
        const val TRAIT_HANDLE_BATCH = 500
    }
}
