package com.rarible.protocol.union.meta.loader.executor

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.download.DownloadException
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadNotifier
import com.rarible.protocol.union.enrichment.meta.downloader.Downloader
import kotlinx.coroutines.awaitAll
import org.slf4j.LoggerFactory

/**
 * Async data download executor, end point of entire download pipeline.
 */
sealed class DownloadExecutor<T>(
    private val repository: DownloadEntryRepository<T>,
    private val downloader: Downloader<T>,
    private val notifier: DownloadNotifier<T>,
    private val pool: DownloadPool,
    private val metrics: DownloadMetrics,
    private val maxRetries: Int,
) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(javaClass)

    abstract val type: String

    abstract fun getBlockchain(task: DownloadTask): BlockchainDto

    suspend fun execute(tasks: List<DownloadTask>) {
        tasks.map {
            pool.submitAsync { execute(it) }
        }.awaitAll()
    }

    private suspend fun execute(task: DownloadTask) {
        val current = repository.get(task.id) ?: getDefault(task)
        if (current.succeedAt != null && task.scheduledAt.isBefore(current.succeedAt)) {
            metrics.onSkippedTask(getBlockchain(task), type, task.pipeline)
            return
        }

        try {
            val data = downloader.download(task.id)
            onSuccess(task, data)
        } catch (e: DownloadException) {
            onFail(task, e.message)
        } catch (e: Exception) {
            logger.error(
                "Unexpected exception while downloading data for {} task {} ()",
                type, task.id, task.pipeline, e
            )
            onFail(task, e.message)
        }
    }

    private suspend fun onSuccess(task: DownloadTask, data: T) {
        // For successful case we should rewrite current data anyway
        val saved = repository.update(task.id) { exist ->
            val current = exist ?: getDefault(task)
            current.withSuccessInc(data)
        }

        saved?.let { notifier.notify(saved) }

        metrics.onSuccessfulTask(getBlockchain(task), type, task.pipeline)
        logger.info("Data download SUCCEEDED for {} task: {} ()", type, task.id, task.pipeline)
    }

    private suspend fun onFail(task: DownloadTask, errorMessage: String?) {
        val saved = repository.update(task.id) { exist ->
            val current = exist ?: getDefault(task)

            val failed = current.withFailInc(errorMessage)

            val isRetryLimitExceeded = failed.retries >= maxRetries
            val status = if (isRetryLimitExceeded) DownloadStatus.FAILED else DownloadStatus.RETRY

            val updated = when (failed.status) {
                // Nothing to do here, we don't want to replace existing data, just update fail counters
                DownloadStatus.SUCCESS, DownloadStatus.FAILED -> failed
                // Failed on retry, just update status, retry counter should be managed by job
                // Status can be changed here if retry limit exceeded
                DownloadStatus.RETRY -> failed.copy(status = status)
                // That was first download, set retry counter as 0 (never retried before)
                // SCHEDULE can turn into FAILED only if we set retry policy with 0 retries
                DownloadStatus.SCHEDULED -> failed.copy(status = status, retries = 0, retriedAt = nowMillis())
            }

            markStatus(task, status)
            updated
        }

        // Never should be null
        saved?.let {
            logger.warn(
                "Data download FAILED for {} task: {} (), status = {}, retries = {}, errorMessage = {}",
                type, saved.id, task.pipeline, saved.status, saved.retries, saved.errorMessage
            )
        }
    }

    private fun markStatus(task: DownloadTask, status: DownloadStatus) {
        when (status) {
            DownloadStatus.FAILED -> metrics.onFailedTask(getBlockchain(task), type, task.pipeline)
            DownloadStatus.RETRY -> metrics.onRetriedTask(getBlockchain(task), type, task.pipeline)
            else -> logger.warn("Incorrect status of failed {} task {} (): {}", type, task.id, task.pipeline, status)
        }
    }

    private fun getDefault(task: DownloadTask): DownloadEntry<T> {
        // This should never happen, originally, at Executor stage entry MUST always exist
        logger.warn("{} entry for task {} () not found, using default state", type, task.id, task.pipeline)
        return DownloadEntry(
            id = task.id,
            status = DownloadStatus.SCHEDULED,
            scheduledAt = task.scheduledAt
        )
    }

    override fun close() {
        pool.close()
    }
}

class ItemDownloadExecutor(
    repository: DownloadEntryRepository<UnionMeta>,
    downloader: Downloader<UnionMeta>,
    notifier: DownloadNotifier<UnionMeta>,
    pool: DownloadPool,
    metrics: DownloadMetrics,
    maxRetries: Int,
) : DownloadExecutor<UnionMeta>(
    repository,
    downloader,
    notifier,
    pool,
    metrics,
    maxRetries,
) {

    override val type = "ITEM"
    override fun getBlockchain(task: DownloadTask) = IdParser.parseItemId(task.id).blockchain

}

class CollectionDownloadExecutor(
    repository: DownloadEntryRepository<UnionCollectionMeta>,
    downloader: Downloader<UnionCollectionMeta>,
    notifier: DownloadNotifier<UnionCollectionMeta>,
    pool: DownloadPool,
    metrics: DownloadMetrics,
    maxRetries: Int,
) : DownloadExecutor<UnionCollectionMeta>(
    repository,
    downloader,
    notifier,
    pool,
    metrics,
    maxRetries,
) {

    override val type = "COLLECTION"
    override fun getBlockchain(task: DownloadTask) = IdParser.parseCollectionId(task.id).blockchain

}