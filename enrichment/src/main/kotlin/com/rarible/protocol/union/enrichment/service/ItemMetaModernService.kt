package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloadService
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaMetrics
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import org.springframework.stereotype.Component

// TODO originally this class is redundant, but we need it for temporary compatibility with legacy service
@Component
class ItemMetaModernService(
    private val downloadService: ItemMetaDownloadService,
    private val metrics: ItemMetaMetrics,
) : ItemMetaService {

    override suspend fun get(itemIds: List<ItemIdDto>, pipeline: ItemMetaPipeline): Map<ItemIdDto, UnionMeta> {
        // Since all data now stored together with entity-owner, there is no need to support batch get
        // But if this is called, it means there are some items with missing meta
        // TODO rename this method to schedule (List<>) and implement batch schedule after the migration
        itemIds.forEach {
            schedule(it, pipeline, false)
            // TODO after switching to meta pipeline should be moved to EnrichmentItemService
            metrics.onMetaCacheMiss(it.blockchain)
        }
        return emptyMap()
    }

    override suspend fun get(itemId: ItemIdDto, sync: Boolean, pipeline: ItemMetaPipeline): UnionMeta? {
        return downloadService.get(itemId, sync, pipeline.pipeline)
    }

    override suspend fun download(itemId: ItemIdDto, pipeline: ItemMetaPipeline, force: Boolean): UnionMeta? {
        return downloadService.download(itemId, pipeline.pipeline, force)
    }

    override suspend fun schedule(itemId: ItemIdDto, pipeline: ItemMetaPipeline, force: Boolean) {
        return downloadService.schedule(itemId, pipeline.pipeline, force)
    }

    override suspend fun save(itemId: ItemIdDto, meta: UnionMeta) {
        downloadService.save(itemId, meta)
    }

}