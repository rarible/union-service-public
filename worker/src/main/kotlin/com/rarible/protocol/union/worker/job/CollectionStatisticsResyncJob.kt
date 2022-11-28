package com.rarible.protocol.union.worker.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.union.worker.task.CollectionStatisticsResyncTask
import com.rarible.protocol.union.worker.config.WorkerProperties
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.time.delay

class CollectionStatisticsResyncJob(
    properties: WorkerProperties,
    meterRegistry: MeterRegistry,
    private val taskRepository: TaskRepository
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.collectionStatisticsResync.rate,
        errorDelay = properties.collectionStatisticsResync.rate
    ),
    workerName = "collection-statistics-resync-job"
) {
    private val taskType = CollectionStatisticsResyncTask.TYPE
    private val param = properties.collectionStatisticsResync.limit.toString()

    override suspend fun handle() {
        val prevTask = taskRepository.findByTypeAndParam(taskType, param).awaitSingleOrNull()
        when {
            prevTask == null -> {
                taskRepository.save(
                    Task(
                        type = taskType,
                        param = param,
                        running = false
                    )
                ).awaitSingle()
                logger.info("Task with type={} was started successfully", taskType)
            }

            prevTask.lastStatus == TaskStatus.COMPLETED -> {
                taskRepository.save(prevTask.copy(lastStatus = TaskStatus.NONE)).awaitSingle()
                logger.info("Task with type={} was started successfully", taskType)
            }

            prevTask.lastStatus == TaskStatus.NONE -> {
                logger.warn("Task with type={} has been started already", taskType)
            }

            else -> {
                logger.error(
                    "Task with type={} can't be started since previous one completed with inappropriate status={}",
                    taskType,
                    prevTask.lastStatus
                )
            }
        }

        delay(pollingPeriod)
    }
}
