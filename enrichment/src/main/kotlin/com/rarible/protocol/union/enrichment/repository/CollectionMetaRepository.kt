package com.rarible.protocol.union.enrichment.repository

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import com.rarible.protocol.union.enrichment.model.ShortCollection
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CollectionMetaRepository(
    private val collectionRepository: CollectionRepository
) : DownloadEntryRepository<UnionCollectionMeta> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun update(
        entryId: String,
        isUpdateRequired: (current: DownloadEntry<UnionCollectionMeta>?) -> Boolean,
        updateEntry: (current: DownloadEntry<UnionCollectionMeta>?) -> DownloadEntry<UnionCollectionMeta>
    ): DownloadEntry<UnionCollectionMeta>? = optimisticLock {
        val collectionId = ShortCollectionId.of(entryId)
        val collection = collectionRepository.get(collectionId) ?: ShortCollection.empty(collectionId)
        if (isUpdateRequired(collection.metaEntry)) {
            val updated = updateEntry(collection.metaEntry)
            logger.info("Updating Collection [{}] with meta entry having status {}", entryId, updated.status)
            collectionRepository.save(collection.withMeta(updated))
            updated
        } else {
            null
        }
    }

    override suspend fun get(id: String): DownloadEntry<UnionCollectionMeta>? {
        val collectionId = ShortCollectionId.of(id)
        return collectionRepository.get(collectionId)?.metaEntry
    }

    override suspend fun getAll(ids: Collection<String>): List<DownloadEntry<UnionCollectionMeta>> {
        return collectionRepository.getAll(ids.map { ShortCollectionId.of(it) }).mapNotNull { it.metaEntry }
    }
}