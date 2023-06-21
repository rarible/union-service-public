package com.rarible.protocol.union.worker.job.meta

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.job.JobHandler
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.union.enrichment.repository.MetaRefreshRequestRepository
import com.rarible.protocol.union.worker.config.WorkerProperties
import com.rarible.protocol.union.worker.kafka.LagService
import com.rarible.protocol.union.worker.metrics.MetaRefreshMetrics
import com.rarible.protocol.union.worker.task.meta.RefreshMetaSimpleHashTask
import com.rarible.protocol.union.worker.task.meta.RefreshMetaTask
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
@ConditionalOnProperty(name = ["worker.metaRefresh.enabled"], havingValue = "true")
class RefreshMetaTaskSchedulingJob(
    properties: WorkerProperties,
    meterRegistry: MeterRegistry,
    private val handler: RefreshMetaTaskSchedulingJobHandler,
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.metaRefresh.rate,
        errorDelay = properties.metaRefresh.rate,
    ),
    workerName = "meta_refresh_request_job"
) {
    override suspend fun handle() {
        withContext(NonCancellable) {
            logger.info("Starting RefreshMetaTaskSchedulingJob")
            handler.handle()
            delay(pollingPeriod)
            logger.info("Finished RefreshMetaTaskSchedulingJob")
        }
    }
}

@ExperimentalCoroutinesApi
@OptIn(FlowPreview::class)
@Component
class RefreshMetaTaskSchedulingJobHandler(
    private val taskRepository: TaskRepository,
    properties: WorkerProperties,
    private val metaRefreshRequestRepository: MetaRefreshRequestRepository,
    private val lagService: LagService,
    private val metaRefreshSchedulingService: MetaRefreshSchedulingService,
    private val metaRefreshMetrics: MetaRefreshMetrics,
) : JobHandler {
    private val concurrency: Int = properties.metaRefresh.concurrency

    override suspend fun handle() {
        val runningTasks = taskRepository.findByRunning(true)
            .filterTasks()
            .asFlow().toList()
        metaRefreshMetrics.runningSize(runningTasks.size)
        metaRefreshMetrics.queueSize(metaRefreshRequestRepository.countNotScheduled())
        if (runningTasks.size >= concurrency) {
            logger.info(
                "Too many tasks are running already ${runningTasks.size}. Concurrency: $concurrency. " +
                    "Skipping RefreshMetaTaskSchedulingJob"
            )
            return
        }
        if (!lagService.isLagOk()) {
            logger.info("Lag is too big. Skipping RefreshMetaTaskSchedulingJob")
            return
        }
        val tasksToStart = concurrency - runningTasks.size
        val collections = metaRefreshRequestRepository.findToScheduleAndUpdate(tasksToStart)
        collections.forEach {
            metaRefreshSchedulingService.scheduleTask(it)
        }
        deleteFinishedTasks()
    }

    private suspend fun deleteFinishedTasks() {
        TASKS_TYPES.forEach { type ->
            taskRepository.findByTypeAndParamRegex(type, ".*").filter {
                !it.running && it.lastStatus != TaskStatus.NONE
            }.asFlow().collect {
                taskRepository.delete(it).awaitSingleOrNull()
            }
        }
    }

    private fun Flux<Task>.filterTasks(): Flux<Task> = this.filter { it.type in TASKS_TYPES }

    companion object {
        private val logger = LoggerFactory.getLogger(RefreshMetaTaskSchedulingJobHandler::class.java)
        private val TASKS_TYPES =
            setOf(RefreshMetaTask.META_REFRESH_TASK, RefreshMetaSimpleHashTask.META_REFRESH_SIMPLEHASH_TASK)
    }
}
