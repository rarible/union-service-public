package com.rarible.protocol.union.worker.job.meta

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.enrichment.model.MetaRefreshRequest
import com.rarible.protocol.union.worker.task.meta.RefreshMetaSimpleHashTask
import com.rarible.protocol.union.worker.task.meta.RefreshMetaTask
import com.rarible.protocol.union.worker.task.meta.RefreshMetaTaskParam
import com.rarible.protocol.union.worker.task.meta.RefreshSimpleHashTaskParam
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MetaRefreshSchedulingService(
    private val taskRepository: TaskRepository,
    private val objectMapper: ObjectMapper
) {

    suspend fun scheduleTask(request: MetaRefreshRequest) {
        logger.info("Scheduling collection refresh $request")
        val (jobParam, type) = when {
            request.withSimpleHash -> Pair(
                RefreshSimpleHashTaskParam(collectionId = request.collectionId),
                RefreshMetaSimpleHashTask.META_REFRESH_SIMPLEHASH_TASK
            )

            else -> Pair(
                RefreshMetaTaskParam(
                    collectionId = request.collectionId,
                    full = request.full,
                    priority = request.priority
                ),
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
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MetaRefreshSchedulingService::class.java)
    }
}
