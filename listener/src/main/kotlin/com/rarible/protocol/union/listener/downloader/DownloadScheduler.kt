package com.rarible.protocol.union.listener.downloader

import com.mongodb.DuplicateKeyException
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException

/**
 * Scheduler takes intermediate place in entire downloader pipeline.
 * It's goal to split incoming tasks from single pipe to other pipes
 * in order to prioritise task execution. It also creates initial states
 * for entries never downloaded before.
 */
abstract class DownloadScheduler<T>(
    private val router: DownloadTaskRouter,
    private val repository: DownloadEntryRepository<T>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun schedule(task: DownloadTask) {
        schedule(listOf(task))
    }

    suspend fun schedule(tasks: Collection<DownloadTask>) {

        val created = createInitialEntries(tasks)

        // Here we pass only forced tasks or just created. Duplicated tasks can be here
        // as a result of multiple simultaneous requests from API
        val deduplicated = tasks.filter {
            it.force || created.contains(it.id)
        }

        deduplicated.groupBy { it.pipeline }.forEach {
            router.send(it.value, it.key)
        }
    }

    private suspend fun createInitialEntries(tasks: Collection<DownloadTask>): Set<String> {
        // Initial download, entry should be created here in order to do not perform
        // such write queries during service's work (in API, for example)
        val ids = tasks.map { it.id }.toSet()
        val exist = repository.getAll(ids).associateByTo(HashMap()) { it.id }

        val notFound = tasks.filter { exist[it.id] == null }
            .groupBy { it.id }

        val created = HashSet<String>()

        // Potentially there could be several tasks for same entry
        notFound.forEach { group ->
            val earliest = group.value.minByOrNull { it.scheduledAt }!!
            val initialEntry = DownloadEntry<T>(
                id = earliest.id,
                status = DownloadStatus.SCHEDULED,
                scheduledAt = earliest.scheduledAt
            )
            val id = initialEntry.id

            try {
                repository.save(initialEntry)
                logger.info("Initial entry created: id={}, scheduledAt={}", id, initialEntry.scheduledAt)
                created.add(id)
            } catch (e: DuplicateKeyException) {
                // If initial entry has been created somewhere else (the only case - force sync meta fetch via API),
                // we can just skip this exception
                logger.info("Entry with key {} already exist", id)
            } catch (e: OptimisticLockingFailureException) {
                logger.info("Entry with key {} already updated", id)
            }
        }

        return created
    }
}