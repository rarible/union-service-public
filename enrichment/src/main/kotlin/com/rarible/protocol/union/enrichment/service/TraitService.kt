package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.repository.TraitRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TraitService(
    private val itemRepository: ItemRepository,
    private val traitRepository: TraitRepository
) {
    suspend fun recalculateTraits(collectionId: EnrichmentCollectionId) {
        traitRepository.deleteAllByCollection(collectionId)
        logger.info("Recalculate traits for collection: $collectionId")
        val traits = itemRepository.getTraitsByCollection(collectionId = collectionId)
        traits.chunked(1000).forEach { chunk ->
            traitRepository.insertAll(chunk)
        }
        // TODO reindex ES PT-4121
        logger.info("Recalculated traits for collection: $collectionId")
    }

    private val logger = LoggerFactory.getLogger(TraitService::class.java)
}
