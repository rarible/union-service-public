package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.asyncBatchHandle
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.ItemAttributeCountChange
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.TraitRepository
import kotlinx.coroutines.flow.count
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

@Component
class TraitService(
    private val itemRepository: ItemRepository,
    private val traitRepository: TraitRepository,
) {
    suspend fun recalculateTraits(collectionId: EnrichmentCollectionId) {
        traitRepository.deleteAllByCollection(collectionId)
        logger.info("Recalculate traits for collection: $collectionId")
        val traits = itemRepository.getTraitsByCollection(collectionId = collectionId)
        traits.chunked(1000).forEach { chunk ->
            traitRepository.saveAll(chunk)
        }
        // TODO reindex ES PT-4121
        logger.info("Recalculated traits for collection: $collectionId")
    }

    suspend fun deleteWithZeroItemsCount() {
        logger.info("Deleting traits with zero items count")
        val deleted = AtomicInteger(0)
        val time = measureTimeMillis {
            deleted.set(traitRepository.deleteWithZeroItemsCount().count())
            // TODO delete from index as well after implementing PT-4121
        }
        logger.info("Deleted traits with zero items count: ${deleted.get()} time: ${time}ms")
    }

    suspend fun changeItemsCount(
        collectionId: EnrichmentCollectionId,
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
        private val logger = LoggerFactory.getLogger(TraitService::class.java)

        // We don't expect that there will be more than 500 traits in one collection
        const val TRAIT_HANDLE_BATCH = 500
    }
}
