package com.rarible.protocol.union.enrichment.repository

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.enrichment.converter.EnrichmentCollectionConverter
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import com.rarible.protocol.union.enrichment.meta.item.MetaTrimmer
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CollectionMetaRepository(
    private val metaTrimmer: MetaTrimmer,
    private val collectionRepository: CollectionRepository,
    private val collectionServiceRouter: BlockchainRouter<CollectionService>,
) : DownloadEntryRepository<UnionCollectionMeta> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun update(
        entryId: String,
        isUpdateRequired: (current: DownloadEntry<UnionCollectionMeta>?) -> Boolean,
        updateEntry: (current: DownloadEntry<UnionCollectionMeta>?) -> DownloadEntry<UnionCollectionMeta>
    ): DownloadEntry<UnionCollectionMeta>? = optimisticLock {
        val collectionId = EnrichmentCollectionId.of(entryId)

        val collection = collectionRepository.get(collectionId) ?: run {
            // TODO remove later, there should NOT be a situation
            // when we updating meta for collection which is not in union DB
            logger.warn("Collection not found in Union DB (meta), fetching: {}", collectionId.toDto().fullId())
            val fetched = collectionServiceRouter.getService(collectionId.blockchain)
                .getCollectionById(collectionId.collectionId)
            EnrichmentCollectionConverter.convert(fetched)
        }

        if (isUpdateRequired(collection.metaEntry)) {
            val updated = updateEntry(collection.metaEntry)
            logger.info("Updating Collection [{}] with meta entry having status {}", entryId, updated.status)

            val trimmedMeta = metaTrimmer.trim(updated.data)
            if (trimmedMeta != updated.data) {
                logger.info("Collection with large meta was trimmed: $collectionId")
            }
            collectionRepository.save(collection.withMeta(updated.withData(trimmedMeta)))
            updated
        } else {
            null
        }
    }

    override suspend fun get(id: String): DownloadEntry<UnionCollectionMeta>? {
        val collectionId = EnrichmentCollectionId.of(id)
        return collectionRepository.get(collectionId)?.metaEntry
    }

    override suspend fun getAll(ids: Collection<String>): List<DownloadEntry<UnionCollectionMeta>> {
        return collectionRepository.getAll(ids.map { EnrichmentCollectionId.of(it) }).mapNotNull { it.metaEntry }
    }
}
