package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.nowMillis
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.enrichment.download.DownloadTask
import com.rarible.protocol.union.enrichment.download.DownloadTaskEvent
import com.rarible.protocol.union.enrichment.repository.DownloadTaskRepository
import com.rarible.protocol.union.enrichment.util.optimisticLockWithInitial
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class DownloadTaskService(
    private val downloadTaskRepository: DownloadTaskRepository
) {

    // Task execution should take about 1-2 minutes MAXIMUM
    // TODO ideally should be part of the config
    private val inProgressLimit = Duration.ofMinutes(10)

    suspend fun update(type: String, events: List<DownloadTaskEvent>) {
        val (toUpdate, exist) = optimisticLock {
            val exists = downloadTaskRepository.getByIds(events.map { it.id }).associateBy { it.id }
            val toUpdate = ArrayList<DownloadTaskEvent>(events.size)
            val toInsert = events.filter {
                val current = exists[it.id]
                when {
                    // New task, should be inserted
                    current == null -> true
                    // Task that already in progress will be debounced,
                    // If priority is the same, we don't need to change task
                    !requireUpdate(current, it) -> false
                    // Always "false"
                    else -> !toUpdate.add(it)
                }
            }
            downloadTaskRepository.insert(toInsert.map { it.toTask(type) })
            toUpdate to exists
        }

        // Update priorities for existing tasks
        coroutineScope {
            toUpdate.map { event ->
                async {
                    optimisticLockWithInitial(exist[event.id]) { initial ->
                        val current = initial ?: downloadTaskRepository.get(event.id)
                        // null means 'just downloaded'
                        if (current == null || !requireUpdate(current, event)) {
                            return@optimisticLockWithInitial
                        }
                        downloadTaskRepository.save(
                            current.copy(
                                priority = event.priority,
                                inProgress = false,
                                startedAt = null
                            )
                        )
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun delete(event: DownloadTaskEvent) {
        downloadTaskRepository.delete(event.id)
    }

    suspend fun getForExecution(type: String, pipeline: String, limit: Int): List<DownloadTask> {
        val result = downloadTaskRepository.findForExecution(type, pipeline)
        return result.mapNotNull {
            try {
                // If there is concurrency problem, let's just skip this task - and handle it in next batch
                downloadTaskRepository.save(it.copy(inProgress = true, startedAt = nowMillis()))
            } catch (ex: OptimisticLockingFailureException) {
                null
            } catch (ex: DuplicateKeyException) {
                null
            }
        }.take(limit).toList()
    }

    suspend fun reactivateStuckTasks(): Long {
        return downloadTaskRepository.reactivateStuckTasks(inProgressLimit)
    }

    suspend fun isPipelineQueueFull(type: String, pipeline: String, maxInQueue: Long): Boolean {
        return downloadTaskRepository.getTaskCountInPipeline(type, pipeline, maxInQueue.toInt()) >= maxInQueue
    }

    private fun requireUpdate(current: DownloadTask, event: DownloadTaskEvent): Boolean {
        if (current.inProgress) {
            val startedAt = current.startedAt ?: return true // Should never be null
            // Means task stuck and it's better to re-activate it
            return Duration.between(startedAt, nowMillis()).compareTo(inProgressLimit) == 1
        }
        return event.priority > current.priority
    }

    private fun DownloadTaskEvent.toTask(type: String) = DownloadTask(
        id = id,
        type = type,
        pipeline = pipeline,
        force = force,
        source = source,
        priority = priority,
        scheduledAt = scheduledAt,
        startedAt = null,
        inProgress = false
    )
}
