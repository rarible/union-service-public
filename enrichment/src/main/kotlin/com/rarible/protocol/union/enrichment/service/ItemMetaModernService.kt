package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloadService

// TODO temporary disabled
// TODO originally this class is redundant, but we need it for temporary compatibility with legacy service
//@Component
class ItemMetaModernService(
    private val downloadService: ItemMetaDownloadService
) : ItemMetaService {

    override suspend fun get(itemIds: List<ItemIdDto>, pipeline: String): Map<ItemIdDto, UnionMeta> {
        return downloadService.get(itemIds, pipeline)
    }

    override suspend fun get(itemId: ItemIdDto, sync: Boolean, pipeline: String): UnionMeta? {
        return downloadService.get(itemId, sync, pipeline)
    }

    override suspend fun download(itemId: ItemIdDto, pipeline: String, force: Boolean): UnionMeta? {
        return downloadService.download(itemId, pipeline, force)
    }

    override suspend fun schedule(itemId: ItemIdDto, pipeline: String, force: Boolean) {
        return downloadService.schedule(itemId, pipeline, force)
    }

    override suspend fun save(itemId: ItemIdDto, meta: UnionMeta) {
        return downloadService.save(itemId, meta)
    }

}