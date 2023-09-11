package com.rarible.protocol.union.enrichment.meta.downloader

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.download.DownloadEntry
import com.rarible.protocol.union.enrichment.download.DownloadException
import com.rarible.protocol.union.enrichment.download.DownloadStatus
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.download.DownloadTaskSource
import com.rarible.protocol.union.enrichment.model.MetaDownloadPriority
import org.slf4j.LoggerFactory

/**
 * Service for direct operations with downloaded data - get/schedule/download etc.
 * Should be used in API/Listener modules to trigger tasks or retrieve already downloaded data.
 */
abstract class DownloadService<K, T>(
    private val repository: DownloadEntryRepository<T>,
    private val publisher: DownloadTaskPublisher,
    private val downloader: Downloader<T>,
    private val notifier: DownloadNotifier<T>,
    private val metrics: DownloadMetrics
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    abstract val type: String

    abstract fun toId(key: K): String

    abstract fun getBlockchain(key: K): BlockchainDto

    /**
     * Get single downloaded entry or schedule download task (with 'force' or not), if there is no entry.
     * For 'sync' get data will be downloaded immediately - there are two cases:
     * 1. If data downloaded, it will be saved and notification will be sent
     * 2. If data download failed, download task will be scheduled (with 'force' or not) if there is no entry in DB
     */
    protected suspend fun get(key: K, sync: Boolean, pipeline: String): T? {
        val id = toId(key)
        val current = repository.get(id)

        // Entry is successfully downloaded, return it
        if (current != null && current.isDownloaded()) {
            return current.data!!
        }

        // If data isn't downloaded and sync download required, downloading it right here
        if (sync) {
            return download(key, pipeline, false, DownloadTaskSource.INTERNAL)
        }

        // There is no current entry, async scheduling should be performed
        if (current == null) {
            schedule(
                key = key,
                pipeline = pipeline,
                force = false,
                source = DownloadTaskSource.INTERNAL,
                priority = MetaDownloadPriority.ASAP,
            )
        }
        return null
    }

    /**
     * Download and update existing data:
     * 1. If data downloaded, it will be saved and notification will be sent
     * 2. If data download failed, download task will be scheduled (with 'force' or not) if there is no entry in DB
     */
    protected suspend fun download(key: K, pipeline: String, force: Boolean, source: DownloadTaskSource): T? {
        val id = toId(key)
        val blockchain = getBlockchain(key)
        val data = try {
            downloader.download(id)
        } catch (e: DownloadException) {
            val current = repository.get(id)
            if (current != null) {
                // If there is existing entry, we need to update only counters
                logger.warn(
                    "Direct download of {} with ID [{}] failed, failed counters incremented: {}",
                    type, id, e.message
                )
                updateFailed(id, blockchain, e.message)
            } else {
                // Otherwise, schedule async download
                logger.warn(
                    "Direct download of {} with ID [{}] failed, scheduling download: {}",
                    type, id, e.message
                )
                schedule(
                    key = key,
                    pipeline = pipeline,
                    force = force,
                    source = source,
                    priority = MetaDownloadPriority.HIGH,
                )
            }
            return null
        }

        logger.info("Direct download of {} with ID [{}] succeeded, saving entry", type, id)
        LogUtils.addToMdc(Pair("source", source.name)) {
            updateSuccessful(id, blockchain, data)
        }
        return data
    }

    /**
     * Schedule async task to download data. If task is forced, it will be executed anyway.
     * Otherwise, task will be executed only if there is no entry in DB (with any status)
     */
    protected suspend fun schedule(
        key: K,
        pipeline: String,
        force: Boolean,
        source: DownloadTaskSource,
        priority: Int
    ) {
        schedule(
            keys = listOf(key),
            pipeline = pipeline,
            force = force,
            source = source,
            priority = priority
        )
    }

    /**
     * Replace current data with new one. This operation is considered as successful download,
     * so counters in entry will be updated and notification will be sent.
     */
    // TODO also should be removed
    suspend fun save(key: K, data: T) {
        updateSuccessful(toId(key), getBlockchain(key), data)
    }

    private suspend fun schedule(
        keys: Collection<K>,
        pipeline: String,
        force: Boolean,
        source: DownloadTaskSource,
        priority: Int
    ) {
        val tasks = keys.map { key ->
            metrics.onTaskScheduled(getBlockchain(key), type, pipeline, force)
            DownloadTaskEvent(
                id = toId(key),
                pipeline = pipeline,
                force = force,
                scheduledAt = nowMillis(),
                source = source,
                priority = priority
            )
        }
        if (tasks.isNotEmpty()) {
            logger.info("Scheduling {} {} tasks with IDs (first 100): {}", tasks.size, type, tasks.take(100))
            publisher.publish(tasks)
        }
    }

    private suspend fun updateSuccessful(id: String, blockchain: BlockchainDto, data: T) {
        metrics.onRequestSucceed(blockchain, type)
        val updated = repository.update(id) { exist ->
            val current = (exist ?: getDefault(id))
            current.withSuccessInc(data)
        }
        updated?.let { notifier.notify(it) }
    }

    private suspend fun updateFailed(id: String, blockchain: BlockchainDto, errorMessage: String?) {
        metrics.onRequestFailed(blockchain, type)
        repository.update(id) { exist ->
            val current = (exist ?: getDefault(id))
            current.withFailInc(errorMessage)
        }
    }

    private fun getDefault(id: String): DownloadEntry<T> {
        return DownloadEntry(
            id = id,
            status = DownloadStatus.SCHEDULED,
            scheduledAt = nowMillis()
        )
    }
}
