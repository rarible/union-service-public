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
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaDownloader
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadNotifier
import com.rarible.protocol.union.enrichment.meta.downloader.Downloader
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaDownloader
import kotlinx.coroutines.awaitAll
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Async data download executor, end point of entire download pipeline.
 */
sealed class DownloadExecutor<T>(
    private val repository: DownloadEntryRepository<T>,
    private val downloader: Downloader<T>,
    private val notifier: DownloadNotifier<T>,
    private val pool: DownloadPool,
    private val metrics: DownloadExecutorMetrics,
    private val maxRetries: Int,
    private val blockchainExtractor: (id: String) -> BlockchainDto,
) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(javaClass)

    abstract val type: String

    suspend fun execute(tasks: List<DownloadTask>) {
        tasks.map {
            pool.submitAsync { execute(it) }
        }.awaitAll()
    }

    private suspend fun execute(task: DownloadTask) {
        val started = Instant.now()
        val current = repository.get(task.id) ?: getDefault(task)
        if (current.succeedAt != null && task.scheduledAt.isBefore(current.succeedAt)) {
            metrics.onSkippedTask(type, blockchainExtractor(task.id), started, task)
            return
        }

        try {
            val data = download(task.id, current)
            onSuccess(started, task, data)
        } catch (e: DownloadException) {
            onFail(started, task, e.message)
        } catch (e: Exception) {
            logger.error(
                "Unexpected exception while downloading data for {} task {} ({})",
                type, task.id, task.pipeline, e
            )
            onFail(started, task, e.message)
        }
    }

    private suspend fun onSuccess(started: Instant, task: DownloadTask, data: T) {
        // For successful case we should rewrite current data anyway
        val saved = repository.update(task.id) { exist ->
            val current = exist ?: getDefault(task)
            current.withSuccessInc(data)
        }

        saved?.let { notifier.notify(saved) }

        metrics.onSuccessfulTask(type, blockchainExtractor(task.id), started, task)
        logger.info("Data download SUCCEEDED for {} task: {} ({})", type, task.id, task.pipeline)
    }

    protected open suspend fun download(id: String, current: DownloadEntry<T>?): T {
        return downloader.download(id)
    }

    private suspend fun onFail(started: Instant, task: DownloadTask, errorMessage: String?) {
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

            markStatus(started, task, status)
            updated
        }

        // Never should be null
        saved?.let {
            logger.warn(
                "Data download FAILED for {} task: {} ({}), status = {}, retries = {}, errorMessage = {}",
                type, saved.id, task.pipeline, saved.status, saved.retries, saved.errorMessage
            )
        }
    }

    private fun markStatus(started: Instant, task: DownloadTask, status: DownloadStatus) {
        when (status) {
            DownloadStatus.FAILED -> metrics.onFailedTask(type, blockchainExtractor(task.id), started, task)
            DownloadStatus.RETRY -> metrics.onRetriedTask(type, blockchainExtractor(task.id), started, task)
            else -> logger.warn("Incorrect status of failed {} task {} ({}): {}", type, task.id, task.pipeline, status)
        }
    }

    private fun getDefault(task: DownloadTask): DownloadEntry<T> {
        // This should never happen, originally, at Executor stage entry MUST always exist
        logger.warn("{} entry for task {} ({}) not found, using default state", type, task.id, task.pipeline)
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
    downloader: ItemMetaDownloader,
    notifier: DownloadNotifier<UnionMeta>,
    pool: DownloadPool,
    metrics: DownloadExecutorMetrics,
    maxRetries: Int
) : DownloadExecutor<UnionMeta>(
    repository,
    downloader,
    notifier,
    pool,
    metrics,
    maxRetries,
    { IdParser.parseItemId(it).blockchain }

) {

    override val type = downloader.type
}

class CollectionDownloadExecutor(
    repository: DownloadEntryRepository<UnionCollectionMeta>,
    downloader: CollectionMetaDownloader,
    notifier: DownloadNotifier<UnionCollectionMeta>,
    pool: DownloadPool,
    metrics: DownloadExecutorMetrics,
    maxRetries: Int,
) : DownloadExecutor<UnionCollectionMeta>(
    repository,
    downloader,
    notifier,
    pool,
    metrics,
    maxRetries,
    { IdParser.parseCollectionId(it).blockchain }
) {

    override val type = downloader.type
}