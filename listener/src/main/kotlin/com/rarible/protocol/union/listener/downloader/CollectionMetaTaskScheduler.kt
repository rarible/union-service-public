package com.rarible.protocol.union.listener.downloader

import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.CollectionMetaRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class CollectionMetaTaskScheduler(
    @Qualifier("collection.meta.schedule.router")
    metaTaskRouter: MetaTaskRouter,
    repository: CollectionMetaRepository,
    metrics: DownloadSchedulerMetrics
) : DownloadScheduler<UnionCollectionMeta>(metaTaskRouter, repository, metrics) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // TODO duplicated code with CollectionTaskExecutor, refactoring required
    override val type = "Collection"

    override fun getBlockchain(task: DownloadTask) = IdParser.parseCollectionId(task.id).blockchain

    override suspend fun logScheduledTask(task: DownloadTask) {
        val collectionId = IdParser.parseCollectionId(task.id)
        LogUtils.addToMdc(collectionId) {
            logger.info("Scheduling $type meta download for $collectionId")
        }
    }
}
