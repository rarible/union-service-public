package com.rarible.protocol.union.worker.job.meta

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskService
import com.rarible.protocol.union.enrichment.model.CollectionMetaRefreshRequest
import com.rarible.protocol.union.worker.task.meta.RefreshMetaTask
import com.rarible.protocol.union.worker.task.meta.RefreshMetaTaskParam
import com.rarible.protocol.union.worker.task.meta.RefreshSimpleHashTask
import com.rarible.protocol.union.worker.task.meta.RefreshSimpleHashTaskParam
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CollectionMetaRefreshSchedulingService(
    private val taskRepository: TaskRepository,
    private val objectMapper: ObjectMapper,
    private val taskService: TaskService,
) {

    suspend fun scheduleTask(collection: CollectionMetaRefreshRequest) {
        logger.info("Scheduling collection refresh $collection")
        val (jobParam, type) = when {
            collection.withSimpleHash -> Pair(
                RefreshSimpleHashTaskParam(collectionId = collection.collectionId),
                RefreshSimpleHashTask.REFRESH_SIMPLEHASH_TASK
            )
            else -> Pair(
                RefreshMetaTaskParam(collectionId = collection.collectionId, full = collection.full),
                RefreshMetaTask.META_REFRESH_TASK
            )
        }
        val param = objectMapper.writeValueAsString(jobParam)
        val existingTask =
            taskRepository.findByTypeAndParam(type = type, param = param)
                .awaitFirstOrNull()
        if (existingTask != null) {
            taskRepository.delete(existingTask).awaitSingle()
        }
        taskRepository.save(
            Task(
                type = type,
                param = param,
                running = false
            )
        ).awaitSingle()
        taskService.runTask(type = type, param = param)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CollectionMetaRefreshSchedulingService::class.java)
    }
}