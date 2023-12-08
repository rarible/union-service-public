package com.rarible.protocol.union.api.service.api

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.producer.UnionInternalCollectionEventProducer
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CollectionApiService(
    private val collectionRepository: CollectionRepository,
    private val unionInternalCollectionEventProducer: UnionInternalCollectionEventProducer,
) {

    suspend fun updateHasTraits(id: EnrichmentCollectionId, hasTraits: Boolean): Boolean {
        logger.info("Setting hasTraits=$hasTraits for collection $id")
        return optimisticLock {
            val collection = collectionRepository.get(id) ?: return@optimisticLock false
            if (collection.hasTraits != hasTraits) {
                collectionRepository.save(collection.copy(hasTraits = hasTraits))
                unionInternalCollectionEventProducer.sendChangeEvent(id.toDto())
            }
            collection.hasTraits != hasTraits
        }
    }

    suspend fun filterHasTraits(ids: List<String>): List<String> {
        val collections = collectionRepository.getAll(ids.map(EnrichmentCollectionId::of))
        return collections.filter { it.hasTraits }.map { it.id.toString() }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CollectionApiService::class.java)
    }
}
