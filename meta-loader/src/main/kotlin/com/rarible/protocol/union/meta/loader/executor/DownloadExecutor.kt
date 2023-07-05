package com.rarible.protocol.union.meta.loader.executor

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.download.DownloadException
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.core.model.download.MetaProviderType
import com.rarible.protocol.union.core.model.download.PartialDownloadException
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaDownloader
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadNotifier
import com.rarible.protocol.union.enrichment.meta.downloader.Downloader
import com.rarible.protocol.union.enrichment.service.EnrichmentBlacklistService
import kotlinx.coroutines.awaitAll
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Async data download executor, end point of entire download pipeline.
 */
sealed class DownloadExecutor<T>(
    protected val enrichmentBlacklistService: EnrichmentBlacklistService,
    private val repository: DownloadEntryRepository<T>,
    private val downloader: Downloader<T>,
    private val notifier: DownloadNotifier<T>,
    private val pool: DownloadPool,
    private val metrics: DownloadExecutorMetrics,
    private val maxRetries: Int,
    protected val blockchainExtractor: (id: String) -> BlockchainDto,
) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(javaClass)

    abstract val type: String

    abstract suspend fun isBlacklisted(task: DownloadTask): Boolean

    suspend fun execute(tasks: List<DownloadTask>) {
        tasks.map {
            pool.submitAsync { execute(it) }
        }.awaitAll()
    }

    private suspend fun execute(task: DownloadTask) {
        val started = Instant.now()
        val current = repository.get(task.id) ?: getDefault(task)
        if (current.succeedAt != null && task.scheduledAt.isBefore(current.succeedAt)) {
            val retry = if (current.status != DownloadStatus.SUCCESS) current.retries else 0
            metrics.onSkippedTask(type, blockchainExtractor(task.id), started, task, retry)
            return
        }

        if (isBlacklisted(task)) {
            onBlacklisted(started, task)
            return
        }

        try {
            val data = download(task.id, current)
            onSuccess(started, task, data)
        } catch (e: DownloadException) {
            onFail(
                started = started,
                task = task,
                errorMessage = e.message,
                data = null,
                downloadStatus = null,
                failedProviders = null,
            )
        } catch (e: PartialDownloadException) {
            onFail(
                started = started,
                task = task,
                errorMessage = e.message,
                data = e.data as T,
                downloadStatus = DownloadStatus.RETRY_PARTIAL,
                failedProviders = e.failedProviders,
            )?.let {
                notifier.notify(it)
            }
        } catch (e: Exception) {
            logger.error(
                "Unexpected exception while downloading data for {} task {} ({})",
                type, task.id, task.pipeline, e
            )
            onFail(
                started = started,
                task = task,
                errorMessage = e.message,
                data = null,
                downloadStatus = null,
                failedProviders = null
            )
        }
    }

    private suspend fun onSuccess(started: Instant, task: DownloadTask, data: T) {
        // For successful case we should rewrite current data anyway
        var retry = 0
        val saved = LogUtils.addToMdc(Pair("source", task.source.name)) {
            repository.update(task.id) { exist ->
                val current = exist ?: getDefault(task)
                // If current.status == success, there is no sense to count its previous retries
                retry = if (current.status != DownloadStatus.SUCCESS) current.retries else 0
                current.withSuccessInc(data)
            }
        }

        saved?.let { notifier.notify(saved) }

        metrics.onSuccessfulTask(type, blockchainExtractor(task.id), started, task, retry)
        logger.info("Data download SUCCEEDED for {} task: {} ({})", type, task.id, task.pipeline)
    }

    protected open suspend fun download(id: String, current: DownloadEntry<T>?): T {
        return downloader.download(id)
    }

    private suspend fun onFail(
        started: Instant,
        task: DownloadTask,
        errorMessage: String?,
        data: T?,
        downloadStatus: DownloadStatus?,
        failedProviders: List<MetaProviderType>?,
    ): DownloadEntry<T>? {
        val saved = repository.update(task.id) { exist ->
            val current = exist ?: getDefault(task)

            val failed = current.withFailInc(errorMessage)

            val isRetryLimitExceeded = failed.retries >= maxRetries
            val status = if (isRetryLimitExceeded) {
                DownloadStatus.FAILED
            } else {
                downloadStatus ?: if (failed.status == DownloadStatus.RETRY_PARTIAL) {
                    DownloadStatus.RETRY_PARTIAL
                } else {
                    DownloadStatus.RETRY
                }
            }

            val updated = when (failed.status) {
                // Nothing to do here, we don't want to replace existing data, just update fail counters
                DownloadStatus.SUCCESS, DownloadStatus.FAILED -> failed
                // Failed on retry, just update status, retry counter should be managed by job
                // Status can be changed here if retry limit exceeded
                DownloadStatus.RETRY,
                DownloadStatus.RETRY_PARTIAL -> failed.copy(
                    status = status,
                    data = data ?: failed.data,
                    failedProviders = failedProviders ?: failed.failedProviders
                )
                // That was first download, set retry counter as 0 (never retried before)
                // SCHEDULE can turn into FAILED only if we set retry policy with 0 retries
                DownloadStatus.SCHEDULED -> failed.copy(
                    status = status,
                    retries = 0,
                    retriedAt = nowMillis(),
                    data = data ?: failed.data,
                    failedProviders = failedProviders ?: failed.failedProviders,
                )
            }

            markStatus(started, task, status, failed.retries)
            updated
        }

        // Never should be null
        return saved?.apply {
            logger.warn(
                "Data download FAILED for {} task: {} ({}), status = {}, retries = {}, errorMessage = {}",
                type, saved.id, task.pipeline, saved.status, saved.retries, saved.errorMessage
            )
        }
    }

    private suspend fun onBlacklisted(started: Instant, task: DownloadTask) {
        val saved = repository.update(task.id) { exist ->
            val current = exist ?: getDefault(task)
            val updated = current
                .withFailInc("Blacklisted")
                .copy(status = DownloadStatus.FAILED, retriedAt = nowMillis())

            markStatus(started, task, updated.status, updated.retries)
            updated
        }

        // Never should be null
        saved?.let {
            logger.warn(
                "Data download ABORTED for {} task: {} ({}) - blacklisted",
                type, saved.id, task.pipeline,
            )
        }
    }

    private fun markStatus(started: Instant, task: DownloadTask, status: DownloadStatus, retry: Int) {
        when (status) {
            DownloadStatus.FAILED -> metrics.onFailedTask(type, blockchainExtractor(task.id), started, task, retry)
            DownloadStatus.RETRY,
            DownloadStatus.RETRY_PARTIAL -> metrics.onRetriedTask(
                type,
                blockchainExtractor(task.id),
                started,
                task,
                retry
            )
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
    enrichmentBlacklistService: EnrichmentBlacklistService,
    repository: DownloadEntryRepository<UnionMeta>,
    downloader: Downloader<UnionMeta>,
    notifier: DownloadNotifier<UnionMeta>,
    pool: DownloadPool,
    metrics: DownloadExecutorMetrics,
    maxRetries: Int
) : DownloadExecutor<UnionMeta>(
    enrichmentBlacklistService,
    repository,
    downloader,
    notifier,
    pool,
    metrics,
    maxRetries,
    { IdParser.parseItemId(it).blockchain }
) {

    override val type = downloader.type
    override suspend fun isBlacklisted(task: DownloadTask): Boolean {
        val blockchain = blockchainExtractor(task.id)
        if (blockchain == BlockchainDto.SOLANA) {
            return false
        }
        val collectionId = task.id.substringBeforeLast(":")
        return enrichmentBlacklistService.isBlacklisted(collectionId)
    }
}

class CollectionDownloadExecutor(
    enrichmentBlacklistService: EnrichmentBlacklistService,
    repository: DownloadEntryRepository<UnionCollectionMeta>,
    downloader: CollectionMetaDownloader,
    notifier: DownloadNotifier<UnionCollectionMeta>,
    pool: DownloadPool,
    metrics: DownloadExecutorMetrics,
    maxRetries: Int,
) : DownloadExecutor<UnionCollectionMeta>(
    enrichmentBlacklistService,
    repository,
    downloader,
    notifier,
    pool,
    metrics,
    maxRetries,
    { IdParser.parseCollectionId(it).blockchain }
) {

    override val type = downloader.type
    override suspend fun isBlacklisted(task: DownloadTask) = false
}