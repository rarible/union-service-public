package com.rarible.protocol.union.enrichment.meta.downloader

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.download.DownloadException
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.enrichment.util.optimisticLockWithInitial

/**
 * Service for direct operations with downloaded data - get/schedule/download etc.
 * Should be used in API/Listener modules to trigger tasks or retrieve already downloaded data.
 */
abstract class DownloadService<K, T>(
    private val repository: DownloadEntryRepository<T>,
    private val publisher: DownloadTaskPublisher,
    private val downloader: Downloader<T>,
    private val notifier: DownloadNotifier<T>,
    // TODO metrics class need here
) {

    abstract fun toId(key: K): String

    /**
     * Finds all existing entries and takes data from successfully downloaded.
     * For non-existing records download tasks will be scheduled without 'force' flag to avoid duplicated tasks
     */
    suspend fun get(keys: Collection<K>, pipeline: String): Map<K, T> {
        val ids = keys.associateBy { toId(it) }

        val result = LinkedHashMap<K, T>(ids.size)
        val notFound = HashSet<String>(ids.keys)

        val found = repository.getAll(ids.keys)
        found.forEach {
            notFound.remove(it.id)
            if (it.isDownloaded()) {
                result[ids[it.id]!!] = it.data!!
            }
        }

        // TODO add metrics (hit/miss)

        schedule(notFound, pipeline, false)

        return result
    }

    /**
     * Get single downloaded entry or schedule download task (with 'force' or not), if there is no entry.
     * For 'sync' get data will be downloaded immediately - there are two cases:
     * 1. If data downloaded, it will be saved and notification will be sent
     * 2. If data download failed, download task will be scheduled (with 'force' or not) if there is no entry in DB
     */
    suspend fun get(key: K, sync: Boolean, pipeline: String): T? {
        val id = toId(key)
        val current = repository.get(id)

        // Entry is successfully downloaded, return it
        if (current != null && current.isDownloaded()) {
            return current.data!!
        }
        // TODO add metrics (hit/miss)

        // If data isn't downloaded and sync download required, downloading it right here
        if (sync) {
            return download(key, pipeline, false)
        }

        // There is no current entry, async scheduling should be performed
        if (current == null) {
            schedule(key, pipeline, false)
        }
        return null
    }

    /**
     * Download and update existing data:
     * 1. If data downloaded, it will be saved and notification will be sent
     * 2. If data download failed, download task will be scheduled (with 'force' or not) if there is no entry in DB
     */
    suspend fun download(key: K, pipeline: String, force: Boolean): T? {
        val id = toId(key)
        val data = try {
            downloader.download(id)
        } catch (e: DownloadException) {
            val current = repository.get(id)
            if (current != null) {
                // If there is existing entry, we need to update only counters
                updateFailed(id, e.message, null)
            } else {
                // Otherwise, schedule async download
                schedule(key, pipeline, force)
            }
            return null
        }

        updateSuccessful(id, data, null)
        return data
    }

    /**
     * Schedule async task to download data. If task is forced, it will be executed anyway.
     * Otherwise, task will be executed only if there is no entry in DB (with any status)
     */
    suspend fun schedule(key: K, pipeline: String, force: Boolean) {
        schedule(listOf(toId(key)), pipeline, force)
    }

    /**
     * Replace current data with new one. This operation is considered as successful download,
     * so counters in entry will be updated and notification will be sent.
     */
    suspend fun save(key: K, data: T) {
        updateSuccessful(toId(key), data, null)
    }

    private suspend fun schedule(ids: Collection<String>, pipeline: String, force: Boolean) {
        val tasks = ids.map {
            DownloadTask(
                id = it,
                pipeline = pipeline,
                force = force,
                scheduledAt = nowMillis()
            )
        }
        if (tasks.isNotEmpty()) {
            publisher.publish(tasks)
        }
    }

    private suspend fun updateSuccessful(
        id: String,
        data: T,
        current: DownloadEntry<T>?
    ) = optimisticLockWithInitial(current) { initial ->
        val exist = initial ?: getOrDefault(id)
        val updated = exist.withSuccessInc(data)
        repository.save(updated)
        notifier.notify(updated)
    }

    private suspend fun updateFailed(
        id: String,
        errorMessage: String?,
        current: DownloadEntry<T>?
    ) = optimisticLockWithInitial(current) { initial ->
        val exist = initial ?: getOrDefault(id)
        val failed = exist.withFailInc(errorMessage)
        repository.save(failed)
    }

    private suspend fun getOrDefault(id: String): DownloadEntry<T> {
        repository.get(id)?.let { return it }

        return DownloadEntry(
            id = id,
            status = DownloadStatus.SCHEDULED,
            scheduledAt = nowMillis()
        )
    }
}

