package com.rarible.protocol.union.listener.downloader

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import com.rarible.protocol.union.enrichment.service.DownloadTaskService
import org.springframework.stereotype.Component

@Component
class ItemMetaTaskScheduler(
    downloadTaskService: DownloadTaskService,
    repository: ItemMetaRepository,
    metrics: DownloadSchedulerMetrics
) : DownloadScheduler<UnionMeta>(downloadTaskService, repository, metrics) {
    // TODO duplicated code with ItemTaskExecutor, refactoring required
    override val type = "item"
    override fun getBlockchain(task: DownloadTaskEvent) = IdParser.parseItemId(task.id).blockchain

    override suspend fun schedule(task: DownloadTaskEvent) {
        super.schedule(changePipeline(task))
    }

    override suspend fun schedule(tasks: Collection<DownloadTaskEvent>) {
        super.schedule(tasks.map { changePipeline(it) })
    }

    // TODO make it in right way
    private fun changePipeline(task: DownloadTaskEvent): DownloadTaskEvent {
        if (getBlockchain(task) != BlockchainDto.BASE) {
            return task
        }
        if (task.pipeline != ItemMetaPipeline.EVENT.pipeline) {
            return task
        }
        return task.copy(pipeline = ItemMetaPipeline.SYNC.pipeline)
    }
}
