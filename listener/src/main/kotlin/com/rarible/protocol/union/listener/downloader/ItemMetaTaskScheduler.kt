package com.rarible.protocol.union.listener.downloader

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemMetaTaskScheduler(
    metaTaskRouter: ItemMetaTaskRouter,
    repository: ItemMetaRepository,
    metrics: DownloadSchedulerMetrics,
    val blockchainRouter: BlockchainRouter<ItemService>
) : DownloadScheduler<UnionMeta>(metaTaskRouter, repository, metrics) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // TODO duplicated code with ItemTaskExecutor, refactoring required
    override val type = "ITEM"
    override fun getBlockchain(task: DownloadTask) = IdParser.parseItemId(task.id).blockchain
    override suspend fun logScheduledTask(task: DownloadTask) {
        val itemId = IdParser.parseItemId(task.id)

        LogUtils.addToMdc(
            itemId,
            blockchainRouter
        ) {
            logger.info("Scheduling item meta download for $itemId")
        }
    }
}