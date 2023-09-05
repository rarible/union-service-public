package com.rarible.protocol.union.listener.downloader

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ItemMetaTaskScheduler(
    @Qualifier("item.meta.schedule.router")
    metaTaskRouter: MetaTaskRouter,
    repository: ItemMetaRepository,
    metrics: DownloadSchedulerMetrics,
    val blockchainRouter: BlockchainRouter<ItemService>
) : DownloadScheduler<UnionMeta>(metaTaskRouter, repository, metrics) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // TODO duplicated code with ItemTaskExecutor, refactoring required
    override val type = "Item"

    override fun getBlockchain(task: DownloadTaskEvent) = IdParser.parseItemId(task.id).blockchain

    override suspend fun logScheduledTask(task: DownloadTaskEvent) {
        val itemId = IdParser.parseItemId(task.id)

        LogUtils.addToMdc(itemId, blockchainRouter) {
            logger.info("Scheduling $type meta download for $itemId")
        }
    }
}
