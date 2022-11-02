package com.rarible.protocol.union.listener.downloader

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import org.springframework.stereotype.Component

@Component
class ItemMetaTaskScheduler(
    router: ItemMetaTaskRouter,
    repository: ItemMetaRepository,
    metrics: DownloadSchedulerMetrics
) : DownloadScheduler<UnionMeta>(router, repository, metrics) {

    // TODO duplicated code with ItemTaskExecutor, refactoring required
    override val type = "ITEM"
    override fun getBlockchain(task: DownloadTask) = IdParser.parseItemId(task.id).blockchain

}