package com.rarible.protocol.union.meta.loader.executor

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.MetaSource
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.download.DownloadEntry
import com.rarible.protocol.union.enrichment.download.DownloadException
import com.rarible.protocol.union.enrichment.download.DownloadStatus
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.download.DownloadTaskSource
import com.rarible.protocol.union.enrichment.download.PartialDownloadException
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaDownloader
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadNotifier
import com.rarible.protocol.union.enrichment.meta.downloader.Downloader
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaRefreshService
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentBlacklistService
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.meta.loader.config.DownloadLimit
import kotlinx.coroutines.Deferred
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

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
    limits: List<DownloadLimit>,
    private val ff: FeatureFlagsProperties,
    protected val blockchainExtractor: (id: String) -> BlockchainDto,
) : AutoCloseable {

    protected val logger = LoggerFactory.getLogger(javaClass)

    private val descLimits = limits.sortedByDescending { it.iterations }

    abstract val type: String

    abstract suspend fun isBlacklisted(task: DownloadTaskEvent): Boolean

    suspend fun submit(task: DownloadTaskEvent, callback: suspend (task: DownloadTaskEvent) -> Unit): Deferred<Unit> {
        return pool.submitAsync {
            execute(task)
            callback(task)
        }
    }

    private suspend fun execute(task: DownloadTaskEvent) {
        val started = Instant.now()
        try {
            val current = repository.get(task.id) ?: getDefault(task)
            if (!checkOutdated(current, task) || !checkAllowed(current, task)) {
                return
            }

            if (isBlacklisted(task)) {
                onBlacklisted(started, task)
                return
            }

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
            val entry = onFail(
                started = started,
                task = task,
                errorMessage = e.message,
                data = e.data as T,
                downloadStatus = DownloadStatus.RETRY_PARTIAL,
                failedProviders = e.failedProviders,
            )
            notifier.notify(entry)
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

    private fun checkOutdated(current: DownloadEntry<T>, task: DownloadTaskEvent): Boolean {
        if (current.updatedAt == null || task.scheduledAt.isAfter(current.updatedAt)) {
            return true
        }
        val retry = if (current.status != DownloadStatus.SUCCESS) current.retries else 0
        metrics.onSkippedTask(type, blockchainExtractor(task.id), nowMillis(), task, retry)
        logger.info(
            "Download {} task for {} scheduled at {}, but entry already updated at {}, skip it",
            type, task.id, task.scheduledAt, current.updatedAt
        )
        return false
    }

    // To prevent spam - some of the items are refreshed too frequently
    private fun checkAllowed(current: DownloadEntry<T>, task: DownloadTaskEvent): Boolean {
        val sinceLastDownload = System.currentTimeMillis() - (current.updatedAt?.toEpochMilli() ?: 0)
        val iterations = current.downloads + current.fails
        val interval = descLimits.find { (iterations >= it.iterations) }?.interval ?: return true
        val result = interval.toMillis() < sinceLastDownload
        if (result) {
            return true
        }
        val retry = if (current.status != DownloadStatus.SUCCESS) current.retries else 0
        metrics.onForbiddenTask(type, blockchainExtractor(task.id), nowMillis(), task, retry)
        logger.info(
            "{} {} has too much downloads ({} ok, {} fails), last update was at {}, skip it",
            type, task.id, current.downloads, current.fails, current.updatedAt
        )
        return false
    }

    private suspend fun onSuccess(started: Instant, task: DownloadTaskEvent, data: T) {
        // For successful case we should rewrite current data anyway
        val retry = AtomicReference(0)
        val previous = AtomicReference<T>(null)
        val saved = LogUtils.addToMdc(Pair("source", task.source.name)) {
            repository.update(task.id) { exist ->
                val current = exist ?: getDefault(task)
                previous.set(current.data)
                // If current.status == success, there is no sense to count its previous retries
                retry.set(if (current.status != DownloadStatus.SUCCESS) current.retries else 0)
                current.withSuccessInc(data)
            }
        }!! // Can't be null here

        notifier.notify(saved)
        onSuccessfulDownload(task, previous.get(), saved.data)

        metrics.onSuccessfulTask(type, blockchainExtractor(task.id), started, task, retry.get())
        markFirstSuccessfulDownload(
            task = task,
            previous = previous.get(),
            retry = retry.get(),
            status = SuccessfulDownloadStatus.FULL
        )
        logger.info("Data download SUCCEEDED for {} task: {} ({})", type, task.id, task.pipeline)
    }

    private suspend fun markFirstSuccessfulDownload(
        task: DownloadTaskEvent,
        previous: T?,
        retry: Int,
        status: SuccessfulDownloadStatus,
    ) {
        // Not a first successful download, not measured
        if (previous != null) {
            return
        }
        val start = getStartDate(task.id)
        start?.let { metrics.onFirstSuccessfulDownload(type, blockchainExtractor(task.id), start, task, retry, status) }
    }

    protected open suspend fun download(id: String, current: DownloadEntry<T>?): T {
        return downloader.download(id)
    }

    private suspend fun onFail(
        started: Instant,
        task: DownloadTaskEvent,
        errorMessage: String?,
        data: T?,
        downloadStatus: DownloadStatus?,
        failedProviders: List<MetaSource>?,
    ): DownloadEntry<T> {
        val retry = AtomicReference(0)
        val previous = AtomicReference<T>(null)
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
                DownloadStatus.SUCCESS -> failed
                // If meta downloaded partially, we can put it instead of empty 'failed' entry
                DownloadStatus.FAILED -> when {
                    data != null -> {
                        failed.copy(
                            status = DownloadStatus.SUCCESS,
                            data = data,
                            failedProviders = failedProviders ?: failed.failedProviders
                        )
                    }

                    else -> failed
                }
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
            previous.set(current.data)
            retry.set(failed.retries)
            updated
        }!! // Never should be null

        if (downloadStatus == DownloadStatus.RETRY_PARTIAL) {
            markFirstSuccessfulDownload(task, previous.get(), retry.get(), SuccessfulDownloadStatus.PARTIAL)
        }

        logger.warn(
            "Data download FAILED for {} task: {} ({}), status = {}, retries = {}, errorMessage = {}",
            type, saved.id, task.pipeline, saved.status, saved.retries, saved.errorMessage
        )
        return saved
    }

    private suspend fun onBlacklisted(started: Instant, task: DownloadTaskEvent) {
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

    private fun markStatus(started: Instant, task: DownloadTaskEvent, status: DownloadStatus, retry: Int) {
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

    private fun getDefault(task: DownloadTaskEvent): DownloadEntry<T> {
        // This should never happen, originally, at Executor stage entry MUST always exist
        logger.warn("{} entry for task {} ({}) not found, using default state", type, task.id, task.pipeline)
        return DownloadEntry(
            id = task.id,
            status = DownloadStatus.SCHEDULED,
            scheduledAt = task.scheduledAt
        )
    }

    protected open suspend fun onSuccessfulDownload(task: DownloadTaskEvent, previous: T?, updated: T?) = Unit

    protected abstract suspend fun getStartDate(id: String): Instant?

    override fun close() {
        pool.close()
    }
}

class ItemDownloadExecutor(
    private val itemMetaRefreshService: ItemMetaRefreshService,
    private val enrichmentItemService: EnrichmentItemService,
    enrichmentBlacklistService: EnrichmentBlacklistService,
    repository: DownloadEntryRepository<UnionMeta>,
    downloader: Downloader<UnionMeta>,
    notifier: DownloadNotifier<UnionMeta>,
    pool: DownloadPool,
    metrics: DownloadExecutorMetrics,
    maxRetries: Int,
    limits: List<DownloadLimit>,
    ff: FeatureFlagsProperties,
    private val simpleHashEnabled: Boolean
) : DownloadExecutor<UnionMeta>(
    enrichmentBlacklistService,
    repository,
    downloader,
    notifier,
    pool,
    metrics,
    maxRetries,
    limits,
    ff,
    { IdParser.parseItemId(it).blockchain }
) {

    override val type = downloader.type

    override suspend fun isBlacklisted(task: DownloadTaskEvent): Boolean {
        val blockchain = blockchainExtractor(task.id)
        if (blockchain == BlockchainDto.SOLANA) {
            return false
        }
        val collectionId = task.id.substringBeforeLast(":")
        return enrichmentBlacklistService.isBlacklisted(collectionId)
    }

    override suspend fun getStartDate(id: String): Instant? {
        return try {
            enrichmentItemService.fetchOrNull(ShortItemId.of(id))?.mintedAt
        } catch (e: Exception) {
            logger.warn("Failed to get Item $id from indexer:", e)
            null
        }
    }

    override suspend fun onSuccessfulDownload(task: DownloadTaskEvent, previous: UnionMeta?, updated: UnionMeta?) {
        // Only EXTERNAL (made by users) refreshes should be checked
        if (task.pipeline != ItemMetaPipeline.REFRESH.pipeline || task.source != DownloadTaskSource.EXTERNAL) {
            return
        }
        try {
            itemMetaRefreshService.scheduleAutoRefreshOnItemMetaChanged(
                itemId = IdParser.parseItemId(task.id),
                previous = previous,
                updated = updated,
                withSimpleHash = simpleHashEnabled
            )
        } catch (e: Exception) {
            logger.warn("Failed to launch full refresh of collection on Item {} mega change", task.id, e)
        }
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
    ff: FeatureFlagsProperties,
    limits: List<DownloadLimit>,
) : DownloadExecutor<UnionCollectionMeta>(
    enrichmentBlacklistService,
    repository,
    downloader,
    notifier,
    pool,
    metrics,
    maxRetries,
    limits,
    ff,
    { IdParser.parseCollectionId(it).blockchain }
) {

    override val type = downloader.type

    override suspend fun isBlacklisted(task: DownloadTaskEvent) = false

    override suspend fun getStartDate(id: String): Instant? {
        // TODO update if we can determine Collection createdAt date
        return null
    }
}
