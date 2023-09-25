package com.rarible.protocol.union.listener.downloader

import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.repository.CollectionMetaRepository
import com.rarible.protocol.union.enrichment.service.DownloadTaskService
import org.springframework.stereotype.Component

@Component
class CollectionMetaTaskScheduler(
    downloadTaskService: DownloadTaskService,
    repository: CollectionMetaRepository,
    metrics: DownloadSchedulerMetrics
) : DownloadScheduler<UnionCollectionMeta>(downloadTaskService, repository, metrics) {
    // TODO duplicated code with CollectionTaskExecutor, refactoring required
    override val type = "Collection"
    override fun getBlockchain(task: DownloadTaskEvent) = IdParser.parseCollectionId(task.id).blockchain
}
