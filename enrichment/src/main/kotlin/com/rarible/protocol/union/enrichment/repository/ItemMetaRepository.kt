package com.rarible.protocol.union.enrichment.repository

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemMetaRepository(
    private val itemRepository: ItemRepository,
) : DownloadEntryRepository<UnionMeta> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun update(
        entryId: String,
        isUpdateRequired: (current: DownloadEntry<UnionMeta>?) -> Boolean,
        updateEntry: (current: DownloadEntry<UnionMeta>?) -> DownloadEntry<UnionMeta>
    ): DownloadEntry<UnionMeta>? = optimisticLock {
        val itemId = ShortItemId.of(entryId)
        val item = itemRepository.get(itemId) ?: ShortItem.empty(itemId)
        if (isUpdateRequired(item.metaEntry)) {
            val updated = updateEntry(item.metaEntry)
            logger.info("Updating ITEM META [{}] with entry having status {}", entryId, updated.status)
            itemRepository.save(item.withMeta(updated))
            updated
        } else {
            null
        }
    }

    override suspend fun get(id: String): DownloadEntry<UnionMeta>? {
        val itemId = ShortItemId.of(id)
        return itemRepository.get(itemId)?.metaEntry
    }

    override suspend fun getAll(ids: Collection<String>): List<DownloadEntry<UnionMeta>> {
        return itemRepository.getAll(ids.map { ShortItemId.of(it) }).mapNotNull { it.metaEntry }
    }
}