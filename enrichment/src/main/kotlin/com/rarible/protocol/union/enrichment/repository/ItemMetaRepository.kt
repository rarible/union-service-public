package com.rarible.protocol.union.enrichment.repository

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import org.springframework.stereotype.Component

@Component
class ItemMetaRepository(
    private val itemRepository: ItemRepository,
) : DownloadEntryRepository<UnionMeta> {

    override suspend fun save(entry: DownloadEntry<UnionMeta>): DownloadEntry<UnionMeta> {
        val itemId = ShortItemId.of(entry.id)
        val item = itemRepository.get(itemId) ?: ShortItem.empty(itemId)
        itemRepository.save(item.withMeta(entry))
        return entry
    }

    override suspend fun get(id: String): DownloadEntry<UnionMeta>? {
        val itemId = ShortItemId.of(id)
        return itemRepository.get(itemId)?.metaEntry
    }

    override suspend fun getAll(ids: Collection<String>): List<DownloadEntry<UnionMeta>> {
        return itemRepository.getAll(ids.map { ShortItemId.of(it) }).mapNotNull { it.metaEntry }
    }
}