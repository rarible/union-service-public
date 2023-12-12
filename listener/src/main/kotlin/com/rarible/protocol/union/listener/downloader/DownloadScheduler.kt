package com.rarible.protocol.union.listener.downloader

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.download.DownloadEntry
import com.rarible.protocol.union.enrichment.download.DownloadStatus
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import com.rarible.protocol.union.enrichment.service.DownloadTaskService
import org.slf4j.LoggerFactory

/**
 * Scheduler takes intermediate place in entire downloader pipeline.
 * It's goal to split incoming tasks from single pipe to other pipes
 * in order to prioritise task execution. It also creates initial states
 * for entries never downloaded before.
 */
abstract class DownloadScheduler<T>(
    private val downloadTaskService: DownloadTaskService,
    private val repository: DownloadEntryRepository<T>,
    private val metrics: DownloadSchedulerMetrics
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    abstract val type: String
    abstract fun getBlockchain(task: DownloadTaskEvent): BlockchainDto

    open suspend fun schedule(task: DownloadTaskEvent) {
        schedule(listOf(task))
    }

    suspend fun schedule(tasks: Collection<DownloadTaskEvent>) {

        val created = createInitialEntries(tasks)

        // Here we pass only forced tasks or just created. Duplicated tasks can be here
        // as a result of multiple simultaneous requests from API
        val deduplicated = tasks.filter {
            val shouldBeExecuted = it.force || created.contains(it.id)
            if (shouldBeExecuted) {
                metrics.onScheduledTask(getBlockchain(it), type, it.pipeline, it.force)
            } else {
                metrics.onSkippedTask(getBlockchain(it), type, it.pipeline, it.force)
            }
            shouldBeExecuted
        }

        downloadTaskService.update(type, deduplicated)
        tasks.forEach { logger.info("Scheduling $type meta download for ${it.id}") }
    }

    private suspend fun createInitialEntries(tasks: Collection<DownloadTaskEvent>): Set<String> {
        // Initial download, entry should be created here in order to do not perform
        // such write queries during service's work (in API, for example)
        val ids = tasks.map { it.id }.toSet()
        val exist = repository.getAll(ids).associateByTo(HashMap()) { it.id }

        val notFound = tasks.filter {
            val result = exist[it.id] == null
            if (!result && !it.force) {
                logger.info("Initial entry with key {} already exists, skipping it", it.id)
            }
            result
        }.groupBy { it.id }

        // Potentially there could be several tasks for same entry
        val created = notFound.map { group ->
            group.value.minByOrNull { it.scheduledAt }!!
        }.chunked(50).map { chunk ->
            chunk.mapAsync { task ->
                val id = task.id
                val updated = repository.update(id, this::isSchedulingRequired) { createInitialEntry(task) }
                updated?.let { id }
            }.filterNotNull()
        }.flatten().toSet()

        return created
    }

    private fun isSchedulingRequired(current: DownloadEntry<T>?): Boolean {
        // Entry should be created ONLY if there is no existing entry
        current?.let { logger.info("Entry with key {} already updated", current.id) }
        return current == null
    }

    private fun createInitialEntry(task: DownloadTaskEvent): DownloadEntry<T> {
        logger.info("Initial entry created: id={}, scheduledAt={}", task.id, task.scheduledAt)
        return DownloadEntry<T>(
            id = task.id,
            status = DownloadStatus.SCHEDULED,
            scheduledAt = task.scheduledAt
        )
    }
}
