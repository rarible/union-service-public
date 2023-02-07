package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadMetrics
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadService
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import org.springframework.stereotype.Component

@Component
class ItemMetaService(
    repository: ItemMetaRepository,
    publisher: ItemMetaTaskPublisher,
    downloader: ItemMetaDownloader,
    notifier: ItemMetaNotifier,
    metrics: DownloadMetrics
) : DownloadService<ItemIdDto, UnionMeta>(repository, publisher, downloader, notifier, metrics) {

    override val type = "ITEM"
    override fun toId(key: ItemIdDto) = key.fullId()
    override fun getBlockchain(key: ItemIdDto) = key.blockchain

    suspend fun get(
        itemId: ItemIdDto,
        sync: Boolean,
        pipeline: ItemMetaPipeline
    ) = get(itemId, sync, pipeline.pipeline)

    suspend fun download(
        itemId: ItemIdDto,
        pipeline: ItemMetaPipeline,
        force: Boolean
    ) = download(itemId, pipeline.pipeline, force)

    suspend fun schedule(
        itemId: ItemIdDto,
        pipeline: ItemMetaPipeline,
        force: Boolean
    ) = schedule(itemId, pipeline.pipeline, force)

}