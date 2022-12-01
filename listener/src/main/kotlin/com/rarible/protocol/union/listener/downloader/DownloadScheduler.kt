package com.rarible.protocol.union.listener.downloader

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import org.slf4j.LoggerFactory

/**
 * Scheduler takes intermediate place in entire downloader pipeline.
 * It's goal to split incoming tasks from single pipe to other pipes
 * in order to prioritise task execution. It also creates initial states
 * for entries never downloaded before.
 */
abstract class DownloadScheduler<T>(
    private val router: DownloadTaskRouter,
    private val repository: DownloadEntryRepository<T>,
    private val metrics: DownloadSchedulerMetrics
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    abstract val type: String
    abstract fun getBlockchain(task: DownloadTask): BlockchainDto

    suspend fun schedule(task: DownloadTask) {
        schedule(listOf(task))
    }

    suspend fun schedule(tasks: Collection<DownloadTask>) {

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

        deduplicated.groupBy { it.pipeline }.forEach { (pipeline, tasks) ->
            router.send(tasks, pipeline)
        }
    }

    private suspend fun createInitialEntries(tasks: Collection<DownloadTask>): Set<String> {
        // Initial download, entry should be created here in order to do not perform
        // such write queries during service's work (in API, for example)
        val ids = tasks.map { it.id }.toSet()
        val exist = repository.getAll(ids).associateByTo(HashMap()) { it.id }

        val notFound = tasks.filter {
            logger.info("Entry with key {} already exists, debouncing task", it.id)
            exist[it.id] == null
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

    private fun createInitialEntry(task: DownloadTask): DownloadEntry<T> {
        logger.info("Initial entry created: id={}, scheduledAt={}", task.id, task.scheduledAt)
        return DownloadEntry<T>(
            id = task.id,
            status = DownloadStatus.SCHEDULED,
            scheduledAt = task.scheduledAt
        )
    }
}