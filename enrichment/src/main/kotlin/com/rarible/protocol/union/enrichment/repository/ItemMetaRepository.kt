package com.rarible.protocol.union.enrichment.repository

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import com.rarible.protocol.union.enrichment.meta.item.MetaTrimmer
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemMetaRepository(
    private val itemMetaTrimmer: MetaTrimmer,
    private val itemRepository: ItemRepository,
    private val blockchainRouter: BlockchainRouter<ItemService>
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
            val updated = LogUtils.addToMdc(itemId.toDto(), blockchainRouter) {
                val result = updateEntry(item.metaEntry)
                logger.info("Updating Item [{}] with meta entry having status {}", entryId, result.status)
                if (result.data?.toComparable() != item.metaEntry?.data?.toComparable()) {
                    logger.info("Metadata has changed after refresh for item $itemId")
                } else {
                    logger.info("Metadata has not changed after refresh for item $itemId")
                }
                result
            }
            val trimmedMeta = itemMetaTrimmer.trim(updated.data)
            if (trimmedMeta != updated.data) {
                logger.info("Item with large meta was trimmed: $itemId")
            }
            itemRepository.save(item.withMeta(updated.withData(trimmedMeta)))
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
