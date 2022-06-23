package com.rarible.protocol.union.enrichment.meta.downloader

import com.mongodb.DuplicateKeyException
import com.rarible.protocol.union.enrichment.meta.downloader.model.DownloadTask
import org.slf4j.LoggerFactory

/**
 * Scheduler takes intermediate place in entire downloader pipeline.
 * It's goal to split incoming tasks from single pipe to other pipes
 * in order to prioritise task execution. It also creates initial states
 * for entries never downloaded before.
 */
class DownloadScheduler<T>(
    private val sender: DownloadTaskRouter,
    private val repository: DownloadEntryRepository<T>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun schedule(task: DownloadTask) {
        schedule(listOf(task))
    }

    suspend fun schedule(tasks: Collection<DownloadTask>) {
        // TODO PT-48 here we need to skip tasks with records already exists (but pass through forced tasks)
        checkInitial(tasks)

        tasks.groupBy { it.pipeline }.forEach {
            sender.send(it.value, it.key)
        }
    }

    private suspend fun checkInitial(tasks: Collection<DownloadTask>) {
        // Initial download, entry should be created here in order to do not perform
        // such write queries during service's work (in API, for example)
        val ids = tasks.map { it.id }.toSet()
        val exist = repository.getAll(ids).associateByTo(HashMap()) { it.id }

        val notFound = tasks.filter { exist[it.id] == null }
            .groupBy { it.id }

        // Potentially there could be several tasks for same entry
        notFound.forEach { group ->
            val earliest = group.value.minByOrNull { it.scheduledAt }!!
            val initialEntry = DownloadEntry<T>(
                id = earliest.id,
                status = DownloadStatus.SCHEDULED,
                scheduledAt = earliest.scheduledAt
            )

            // TODO add logging
            try {
                repository.save(initialEntry)
            } catch (e: DuplicateKeyException) {
                // If initial entry has been created somewhere else (the only case - force sync meta fetch via API),
                // we can just skip this exception
                logger.info("Entry with key {} already exist", initialEntry.id)
            }
        }
    }
}