package com.rarible.protocol.union.enrichment.custom.collection.provider

import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.producer.UnionInternalCollectionEventProducer
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

@Component
class ArtificialCollectionService(
    private val collectionRepository: CollectionRepository,
    private val producer: UnionInternalCollectionEventProducer
) {

    private val cache = Collections.newSetFromMap(ConcurrentHashMap<CollectionIdDto, Boolean>())

    suspend fun exists(surrogateId: CollectionIdDto): Boolean {
        // Just to avoid unnecessary DB calls after collection has been created
        if (cache.contains(surrogateId)) {
            return true
        }

        val foundInDb = collectionRepository.get(EnrichmentCollectionId(surrogateId)) != null
        if (foundInDb) {
            cache.add(surrogateId)
            return true
        }

        return false
    }

    suspend fun createArtificialCollection(
        originalId: CollectionIdDto,
        surrogateId: CollectionIdDto,
        name: String?,
        structure: UnionCollection.Structure
    ) {
        val original = collectionRepository.get(EnrichmentCollectionId(originalId))
            ?: throw IllegalArgumentException("Can't create sub-collection $surrogateId of $originalId - not found")

        try {
            collectionRepository.save(
                original.copy(
                    collectionId = surrogateId.value,
                    name = name ?: original.name, // Nothing to do with it...
                    structure = structure
                )
            )
            producer.sendChangeEvent(surrogateId)
        } catch (e: DuplicateKeyException) {
            // Nothing to do, somebody already created it
        } catch (e: OptimisticLockingFailureException) {
            // Nothing to do, somebody already created it
        }

        cache.add(surrogateId)
    }
}
