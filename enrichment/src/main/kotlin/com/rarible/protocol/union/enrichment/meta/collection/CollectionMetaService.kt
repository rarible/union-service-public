package com.rarible.protocol.union.enrichment.meta.collection

import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.download.DownloadTaskSource
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadMetrics
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadService
import com.rarible.protocol.union.enrichment.repository.CollectionMetaRepository
import org.springframework.stereotype.Component

@Component
class CollectionMetaService(
    repository: CollectionMetaRepository,
    publisher: CollectionMetaTaskPublisher,
    downloader: CollectionMetaDownloader,
    notifier: CollectionMetaNotifier,
    metrics: DownloadMetrics
) : DownloadService<CollectionIdDto, UnionCollectionMeta>(repository, publisher, downloader, notifier, metrics) {

    override val type = downloader.type
    override fun toId(key: CollectionIdDto) = key.fullId()
    override fun getBlockchain(key: CollectionIdDto) = key.blockchain

    suspend fun get(
        collectionId: CollectionIdDto,
        sync: Boolean,
        pipeline: CollectionMetaPipeline
    ) = get(collectionId, sync, pipeline.pipeline)

    suspend fun download(
        collectionId: CollectionIdDto,
        pipeline: CollectionMetaPipeline,
        force: Boolean
    ) = download(collectionId, pipeline.pipeline, force, DownloadTaskSource.INTERNAL)

    suspend fun schedule(
        collectionId: CollectionIdDto,
        pipeline: CollectionMetaPipeline,
        force: Boolean
    ) = schedule(collectionId, pipeline.pipeline, force, DownloadTaskSource.INTERNAL)
}
